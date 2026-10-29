"""Validate local Markdown/HTML links and heading anchors across the repository."""
from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import unquote,urlsplit
import re,html,sys
ROOT=Path(__file__).resolve().parents[1]
IGNORED={'.git','build','.gradle','.pio','__pycache__','runtime','work','rendered'}
def body(p):
 return re.sub(r'^\s*(```|~~~).*?^\s*\1\s*$', '',p.read_text(encoding='utf-8-sig'),flags=re.M|re.S)
def anchors(p):
 t=body(p);seen={};ids=set(re.findall(r'\b(?:id|name)=["\']([^"\']+)',t))
 for h in re.findall(r'^ {0,3}#{1,6}\s+(.+?)\s*#*\s*$',t,re.M):
  h=re.sub(r'!?\[([^]]+)\]\([^)]*\)',r'\1',h);h=html.unescape(re.sub('<[^>]+>','',h)).replace('`','').lower()
  slug=re.sub(r'[^\w\-\s]','',h);slug=re.sub(r'\s','-',slug);n=seen.get(slug,0);seen[slug]=n+1;ids.add(slug+(f'-{n}' if n else ''))
 return ids
class Links(HTMLParser):
 def __init__(self):super().__init__();self.links=[]
 def handle_starttag(self,tag,attrs):
  self.links.extend(v for k,v in attrs if k in ('src','href') and v)
errors=[];count=0;files=0
for p in ROOT.rglob('*.md'):
 if any(x in IGNORED for x in p.relative_to(ROOT).parts):continue
 files+=1;t=body(p);parser=Links();parser.feed(t)
 links=parser.links+re.findall(r'!?\[[^\n]*?\]\((<[^>]+>|[^\s)]+)(?:\s+"[^"]*")?\)',t)
 for link in links:
  u=urlsplit(html.unescape(link.strip('<>')))
  if u.scheme or u.netloc:continue
  count+=1;target=(p.parent/unquote(u.path)).resolve() if u.path else p
  if not target.is_relative_to(ROOT.resolve()):errors.append((p,'outside repository',link));continue
  if not target.exists():errors.append((p,'missing target',link));continue
  if u.fragment and target.suffix.lower()=='.md' and unquote(u.fragment) not in anchors(target):errors.append((p,'missing anchor',link))
for p,reason,link in errors:print(f'{p.relative_to(ROOT)}: {reason}: {link}')
print(f'{files} Markdown files; {count} local targets; {len(errors)} errors')
sys.exit(bool(errors))
