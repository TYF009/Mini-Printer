package studio.foli.android;

/** Original, offline templates. Preview and print use the same renderer. */
final class TemplateCatalog {
    static final class Template {
        final int id, style, color;
        final String name, category, description, body;
        final boolean checklist;
        Template(int id,String name,String category,String description,String body,boolean checklist,int style,int color){
            this.id=id;this.name=name;this.category=category;this.description=description;this.body=body;this.checklist=checklist;this.style=style;this.color=color;
        }
    }
    static final Template[] ALL={
        new Template(0,"Today's three.","Focus","A little less, a little better.","Finish something good\nTake a proper break\nMake time to create",true,0,0xFFDCEDE0),
        new Template(1,"A fresh thought.","Notes","Give a good idea a place to land.","A small idea can be\nthe start of something good.",false,1,0xFFE2EBF2),
        new Template(2,"Just for you.","Kindness","Say the little things that matter.","You make ordinary days\na little brighter.\n\nI'm glad you're here.",false,2,0xFFF4DFD5),
        new Template(3,"A little list.","Lists","For whatever the day calls for.","Something fresh\nSomething useful\nA little treat",true,0,0xFFEDE9DC),
        new Template(4,"One good thing.","Notes","Keep a small moment from today.","Something that made me smile:\n\nA moment I want to remember:\n\nSomething to look forward to:",false,1,0xFFEAE3F0),
        new Template(5,"You've got this.","Kindness","A small reminder. A little courage.","You don't have to do it all.\n\nJust take the next small step.\n\nI'm cheering for you.",false,2,0xFFF0E6C9),
        new Template(6,"A fresh start.","Focus","Make space for what matters.","One thing to finish\nOne thing to let go\nOne thing just for me",true,1,0xFFDCE9E9),
        new Template(7,"Take a little trip.","Lists","Less forgetting. More exploring.","Keys and wallet\nPhone and charger\nSomething to read\nRoom for a little adventure",true,0,0xFFE0E8F4)
    };
    static Template get(int id){return ALL[Math.max(0,Math.min(ALL.length-1,id))];}
}
