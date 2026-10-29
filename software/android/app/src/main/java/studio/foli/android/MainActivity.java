package studio.foli.android;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.animation.DecelerateInterpolator;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Native Android UI. No web runtime, network service, account or third-party SDK. */
public final class MainActivity extends Activity implements BlePrinter.Listener {
    static final int INK=Color.rgb(33,60,53), GREEN=Color.rgb(48,91,73), MUTED=Color.rgb(94,111,102);
    static final int CANVAS=Color.rgb(252,252,248), MINT=Color.rgb(201,232,215), CARD=Color.rgb(239,242,233);
    private static final int PICK_PHOTO=10, PERMISSIONS=11, ENABLE_BT=12;
    private BlePrinter printer;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private LinearLayout root, content, nav;
    private FrameLayout pageHost, headerHost, footerHost, noticeHost;
    private ScrollView pageScroll;
    private String renderedScreen="", editorOrigin="home", templateFilter="All";
    private final Map<String,Integer> scrollPositions=new HashMap<>();
    private ImageView paperImage;
    private TextView paperInfo, editorTitle, footerInfo, footerAction, undoAction, previewStatus;
    private TextView batteryValue,paperValue,temperatureValue,deviceCopy;
    private final ArrayList<TextView> densityChoices=new ArrayList<>();
    private int paperStyle, paperAlignment;
    private final ExecutorService thumbWorker=Executors.newFixedThreadPool(2);
    private final Map<Integer,Bitmap> templateThumbnails=new HashMap<>();
    private final Map<Integer,ArrayList<ImageView>> thumbnailWaiters=new HashMap<>();
    private final Set<Integer> thumbnailsInFlight=new HashSet<>();
    private final Set<String> favorites=new HashSet<>();
    private final ArrayDeque<DraftState> undoHistory=new ArrayDeque<>();
    private Runnable pendingRender;
    private long latestRenderMs;
    private Dialog activePanel;
    private ImageView sheetPreview;
    private TextView stateText, progressText, chip;
    private ProgressBar progressBar;
    private AlertDialog devicesDialog;
    private ArrayAdapter<String> devicesAdapter;
    private TextView scanStatus;
    private ArrayList<BluetoothDevice> scanDevices=new ArrayList<>();
    private String screen="home", title="Today's three.", body="Finish something good\nTake a proper break\nMake time to create";
    private boolean photoMode=false, checklist=true, dither=true, rendering;
    private volatile boolean destroyed;
    private Bitmap photo;
    private PaperRenderer.Paper paper;
    private int crop=0, turns=0, brightness=0, density=2, typeSize=22;
    private volatile int renderVersion;
    private String renderError="";
    private boolean uncertainPrint;
    private String deviceSnapshot="";

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        printer=new BlePrinter(this,this);
        android.content.SharedPreferences prefs=getPreferences(MODE_PRIVATE);
        title=prefs.getString("title",title);body=prefs.getString("body",body);
        checklist=prefs.getBoolean("checklist",true);photoMode=prefs.getBoolean("photoMode",false);
        dither=prefs.getBoolean("dither",true);brightness=prefs.getInt("brightness",0);
        density=prefs.getInt("density",2);typeSize=prefs.getInt("typeSize",22);crop=prefs.getInt("crop",0);turns=prefs.getInt("turns",0);
        paperStyle=prefs.getInt("paperStyle",0);paperAlignment=prefs.getInt("paperAlignment",0);
        favorites.addAll(prefs.getStringSet("favorites",Collections.emptySet()));
        printer.dirty=prefs.getBoolean("uncertainPrint",false);uncertainPrint=printer.dirty;
        if(photoMode)photo=BitmapFactory.decodeFile(new File(getFilesDir(),"photo.png").getPath());
        if(photo==null)photoMode=false;
        if(state!=null){screen=state.getString("screen","home");editorOrigin=state.getString("editorOrigin","home");templateFilter=state.getString("filter","All");for(String route:new String[]{"home","templates","editor","device"})scrollPositions.put(route,state.getInt("scroll_"+route,0));}
        show();renderPaper();for(TemplateCatalog.Template t:TemplateCatalog.ALL)ensureThumbnail(t);
    }
    int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
    private void gap(LinearLayout l,int size){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(size)));}
    private TextView label(String value,float size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);return t;
    }
    private TextView heading(String value,float size){return label(value,size,INK,true);}
    private boolean motion(){return Build.VERSION.SDK_INT<26||android.animation.ValueAnimator.areAnimatorsEnabled();}
    private void feedback(View view,Runnable action){
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x20305B49),view.getBackground(),bg(Color.WHITE,20)));
        view.setFocusable(true);view.setOnClickListener(v->action.run());
    }
    private TextView pill(String value,boolean selected,Runnable action){
        TextView t=label(value,12,selected?Color.WHITE:GREEN,selected);t.setGravity(Gravity.CENTER);pad(t,17,12,17,12);t.setMinHeight(dp(48));t.setBackground(bg(selected?INK:CARD,24));t.setSelected(selected);feedback(t,action);return t;
    }
    private void paragraph(LinearLayout l,String value){TextView t=label(value,14,MUTED,false);t.setLineSpacing(dp(4),1);l.addView(t);}
    private TextView button(String value,boolean primary,Runnable action){
        TextView b=label(value,14,primary?Color.WHITE:GREEN,true);b.setGravity(Gravity.CENTER);pad(b,16,13,16,13);
        b.setMinHeight(dp(48));b.setBackground(bg(primary?INK:MINT,24));feedback(b,action);b.setClickable(true);b.setFocusable(true);
        b.setContentDescription(value);return b;
    }
    private void fullButton(LinearLayout l,String value,boolean primary,Runnable action){l.addView(button(value,primary,action),new LinearLayout.LayoutParams(-1,-2));}
    private void iconButton(LinearLayout parent,String icon,String name,Runnable action){
        LinearLayout box=col();box.setGravity(Gravity.CENTER);pad(box,5,8,5,8);box.setMinimumHeight(dp(58));
        box.addView(new FoliIcon(this,icon,GREEN),new LinearLayout.LayoutParams(dp(23),dp(23)));gap(box,7);
        TextView caption=label(name,11,MUTED,false);caption.setGravity(Gravity.CENTER);box.addView(caption);box.setContentDescription(name);box.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        feedback(box,()->{if(printer.busy()){message("Let this print finish before editing the next paper.");return;}action.run();});parent.addView(box,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void card(LinearLayout parent,View child,int color){
        LinearLayout box=col();pad(box,18,18,18,18);box.setBackground(bg(color,20));box.addView(child);parent.addView(box,new LinearLayout.LayoutParams(-1,-2));
    }
    private String connectionLabel(){return printer.ready?"FOLI connected":printer.connecting?"Connecting…":"Connect FOLI";}
    private void show(){
        if(destroyed)return;
        boolean routeChanged=!screen.equals(renderedScreen);
        if(pageScroll!=null)scrollPositions.put(renderedScreen,pageScroll.getScrollY());
        stateText=null;progressText=null;progressBar=null;chip=null;
        paperImage=null;paperInfo=null;previewStatus=null;footerInfo=null;footerAction=null;editorTitle=null;undoAction=null;
        batteryValue=paperValue=temperatureValue=deviceCopy=null;densityChoices.clear();
        if(root==null){
            root=col();root.setBackgroundColor(CANVAS);root.setFitsSystemWindows(true);
            headerHost=new FrameLayout(this);pageHost=new FrameLayout(this);noticeHost=new FrameLayout(this);footerHost=new FrameLayout(this);nav=row();
            root.addView(headerHost,new LinearLayout.LayoutParams(-1,-2));root.addView(pageHost,new LinearLayout.LayoutParams(-1,0,1));
            root.addView(footerHost,new LinearLayout.LayoutParams(-1,-2));root.addView(nav,new LinearLayout.LayoutParams(-1,-2));setContentView(root);
        }
        headerHost.removeAllViews();footerHost.removeAllViews();noticeHost.removeAllViews();
        pageScroll=new ScrollView(this);pageScroll.setFillViewport(true);pageScroll.setClipToPadding(false);pageScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        content=col();pad(content,24,screen.equals("editor")?8:20,24,24);pageScroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        switch(screen){case "editor":editor();break;case "templates":templates();break;case "device":device();break;default:home();}
        pageHost.removeAllViews();pageHost.addView(pageScroll,new FrameLayout.LayoutParams(-1,-1));FrameLayout.LayoutParams noticePosition=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);pageHost.addView(noticeHost,noticePosition);
        nav.removeAllViews();nav.setBackgroundColor(CANVAS);pad(nav,8,3,8,3);nav.setVisibility(screen.equals("editor")?View.GONE:View.VISIBLE);
        navItem("home","Create","home");navItem("grid","Templates","templates");navItem("printer","Device","device");
        int restore=scrollPositions.getOrDefault(screen,0);pageScroll.post(()->{if(pageScroll!=null)pageScroll.scrollTo(0,restore);});
        if(routeChanged&&motion()){pageScroll.setAlpha(0);pageScroll.setTranslationY(dp(8));pageScroll.animate().alpha(1).translationY(0).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();}
        renderedScreen=screen;
        updateStatus();
    }
    private void navItem(String ic,String text,String route){
        boolean selected=screen.equals(route)||("home".equals(route)&&"editor".equals(screen));
        LinearLayout box=col();box.setGravity(Gravity.CENTER);pad(box,8,8,8,8);box.setMinimumHeight(dp(60));
        box.addView(new FoliIcon(this,ic,selected?GREEN:MUTED),new LinearLayout.LayoutParams(dp(22),dp(22)));gap(box,6);
        TextView caption=label(text,11,selected?GREEN:MUTED,selected);caption.setGravity(Gravity.CENTER);box.addView(caption);box.setContentDescription(text);box.setSelected(selected);box.setFocusable(true);
        feedback(box,()->{if(!screen.equals(route)){saveDraft();screen=route;show();}});nav.addView(box,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void top(String name){
        LinearLayout top=row();top.addView(heading(name,name.equals("FOLI")?28:24),new LinearLayout.LayoutParams(0,-2,1));
        chip=label(connectionLabel(),10,GREEN,true);pad(chip,12,10,12,10);chip.setMinHeight(dp(48));chip.setGravity(Gravity.CENTER);chip.setBackground(bg(CARD,20));
        feedback(chip,()->{screen="device";show();});top.addView(chip);content.addView(top);gap(content,24);
    }
    private void home(){
        top("FOLI");TextView h=heading("Make a little\nroom for joy.",35);h.setLineSpacing(dp(2),1);content.addView(h);gap(content,12);
        paragraph(content,"A thought. A list. A little surprise.");gap(content,25);
        LinearLayout hero=col();hero.addView(label("START WITH SOMETHING SIMPLE",10,GREEN,true));gap(hero,18);
        LinearLayout heroRow=row();LinearLayout words=col();words.addView(heading("Today, on paper.",22));gap(words,17);
        words.addView(button("Make a note  →",true,()->{photoMode=false;screen="editor";renderPaper();show();}));
        heroRow.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout tiny=col();pad(tiny,10,13,10,13);tiny.setBackground(bg(Color.rgb(255,254,248),3));tiny.setRotation(7);
        tiny.addView(label("TODAY",9,INK,true));gap(tiny,10);tiny.addView(label("□  A little focus\n\n□  A fresh idea\n\n□  Time for you",9,MUTED,false));
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(dp(98),dp(126));tp.setMargins(dp(10),0,0,0);heroRow.addView(tiny,tp);hero.addView(heroRow);card(content,hero,MINT);gap(content,18);
        LinearLayout shortcuts=row();shortcut(shortcuts,"note","Notes",Color.rgb(241,238,229),()->{photoMode=false;screen="editor";renderPaper();show();});
        shortcut(shortcuts,"photo","Photos",Color.rgb(231,239,244),this::pickPhoto);
        shortcut(shortcuts,"grid","Templates",Color.rgb(246,229,222),()->{screen="templates";show();});content.addView(shortcuts);gap(content,28);
        content.addView(heading("A little inspiration",20));gap(content,12);
        LinearLayout inspiration=row();inspiration.addView(new FoliIcon(this,"heart",GREEN),new LinearLayout.LayoutParams(dp(27),dp(27)));
        LinearLayout copy=col();pad(copy,16,0,0,0);copy.addView(heading("Leave a little kindness.",15));gap(copy,7);copy.addView(label("A note can make someone's day.",12,MUTED,false));inspiration.addView(copy);card(content,inspiration,CARD);
        View parent=content.getChildAt(content.getChildCount()-1);parent.setOnClickListener(v->useTemplate(2));parent.setFocusable(true);parent.setContentDescription("Create a kindness note");
        gap(content,24);TextView resume=label("Continue your saved paper  →",13,GREEN,true);pad(resume,0,12,0,12);resume.setMinHeight(dp(48));resume.setOnClickListener(v->{screen="editor";show();});resume.setFocusable(true);content.addView(resume);
    }
    private void shortcut(LinearLayout parent,String ic,String name,int color,Runnable action){
        LinearLayout box=col();pad(box,14,14,10,14);box.setBackground(bg(color,17));
        box.addView(new FoliIcon(this,ic,INK),new LinearLayout.LayoutParams(dp(25),dp(25)));gap(box,17);box.addView(label(name,12,INK,true));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(98),1);if(parent.getChildCount()>0)p.setMargins(dp(10),0,0,0);parent.addView(box,p);
        box.setOnClickListener(v->action.run());box.setFocusable(true);box.setContentDescription(name);
    }
    private void templates(){
        top("Templates");content.addView(heading("A little inspiration.\nMade yours.",29));gap(content,10);paragraph(content,"Pick a paper. Make it personal.");gap(content,18);
        HorizontalScrollView filters=new HorizontalScrollView(this);filters.setHorizontalScrollBarEnabled(false);LinearLayout chips=row();final TextView[] activeFilter={null};
        for(String name:new String[]{"All","Focus","Notes","Kindness","Lists","Saved"}){
            TextView filter=pill(name,templateFilter.equals(name),()->{templateFilter=name;scrollPositions.put("templates",0);if(pageScroll!=null)pageScroll.scrollTo(0,0);show();});
            if(templateFilter.equals(name))activeFilter[0]=filter;
            LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-2,-2);fp.setMargins(0,0,dp(8),0);chips.addView(filter,fp);
        }
        filters.addView(chips);content.addView(filters);filters.post(()->{if(activeFilter[0]!=null)filters.scrollTo(Math.max(0,activeFilter[0].getLeft()-dp(16)),0);});gap(content,20);
        ArrayList<TemplateCatalog.Template> selected=new ArrayList<>();
        for(TemplateCatalog.Template t:TemplateCatalog.ALL)if(templateFilter.equals("All")||templateFilter.equals(t.category)||(templateFilter.equals("Saved")&&favorites.contains(""+t.id)))selected.add(t);
        if(selected.isEmpty()){gap(content,35);content.addView(heading("Your favorites live here.",22));gap(content,10);paragraph(content,"Save a template with the heart button to keep it close.");return;}
        content.addView(label(selected.size()+(selected.size()==1?" LITTLE STARTER":" LITTLE STARTERS"),10,MUTED,true));gap(content,12);
        for(int index=0;index<selected.size();index+=2){
            LinearLayout pair=row();pair.setGravity(Gravity.TOP);
            for(int side=0;side<2;side++){
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);if(side==1)cp.setMargins(dp(12),0,0,0);
                if(index+side>=selected.size()){pair.addView(new View(this),cp);continue;}
                TemplateCatalog.Template t=selected.get(index+side);LinearLayout tile=col();tile.setBackground(bg(Color.WHITE,20));
                LinearLayout art=col();art.setGravity(Gravity.CENTER);pad(art,13,16,13,16);art.setBackground(bg(t.color,18));
                ImageView thumb=new ImageView(this);thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);thumb.setContentDescription("Preview: "+t.name);art.addView(thumb,new LinearLayout.LayoutParams(-1,dp(142)));templateThumb(t,thumb);
                tile.addView(art);LinearLayout words=col();pad(words,12,12,12,10);TextView name=heading(t.name,15);name.setMinHeight(dp(40));words.addView(name);
                LinearLayout meta=row();meta.addView(label(t.category,11,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));
                TextView heart=label(favorites.contains(""+t.id)?"♥":"♡",24,GREEN,false);heart.setGravity(Gravity.CENTER);heart.setMinHeight(dp(48));heart.setMinWidth(dp(48));heart.setContentDescription((favorites.contains(""+t.id)?"Unsave ":"Save ")+t.name);
                feedback(heart,()->{if(!favorites.add(""+t.id))favorites.remove(""+t.id);saveDraft();if(templateFilter.equals("Saved"))show();else{heart.setText(favorites.contains(""+t.id)?"♥":"♡");heart.setContentDescription((favorites.contains(""+t.id)?"Unsave ":"Save ")+t.name);}});meta.addView(heart);words.addView(meta);tile.addView(words);
                tile.setContentDescription("Preview "+t.name);feedback(tile,()->previewTemplate(t));pair.addView(tile,cp);
            }
            content.addView(pair);gap(content,14);
        }
    }
    private void templateThumb(TemplateCatalog.Template t,ImageView target){
        Bitmap cached=templateThumbnails.get(t.id);if(cached!=null){target.setImageBitmap(cached);return;}
        target.setTag(t.id);target.setImageResource(R.drawable.template_placeholder);target.setContentDescription("Preparing preview: "+t.name);
        ArrayList<ImageView> waiters=thumbnailWaiters.get(t.id);if(waiters==null){waiters=new ArrayList<>();thumbnailWaiters.put(t.id,waiters);}waiters.add(target);ensureThumbnail(t);
    }
    private void ensureThumbnail(TemplateCatalog.Template t){
        if(templateThumbnails.containsKey(t.id)||!thumbnailsInFlight.add(t.id))return;
        thumbWorker.execute(()->{try{
            Bitmap raster=PaperRenderer.note(t.name,t.body,20,t.checklist,t.style,t.style==2?1:0);
            Bitmap image=PaperRenderer.finish(raster,false,0).bitmap;
            ui.post(()->{if(destroyed)return;thumbnailsInFlight.remove(t.id);templateThumbnails.put(t.id,image);ArrayList<ImageView> waiting=thumbnailWaiters.remove(t.id);if(waiting!=null)for(ImageView target:waiting)if(Integer.valueOf(t.id).equals(target.getTag())){target.setImageBitmap(image);target.setContentDescription("Preview: "+t.name);if(motion()&&target.isAttachedToWindow()){target.setAlpha(0);target.animate().alpha(1).setDuration(160).start();}}});
        }catch(RuntimeException|OutOfMemoryError ignored){ui.post(()->{if(!destroyed){thumbnailsInFlight.remove(t.id);thumbnailWaiters.remove(t.id);}});}});
    }
    private Dialog sheet(String name,String subtitle,LinearLayout bodyView){
        Dialog dialog=new Dialog(this);activePanel=dialog;dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout shell=col();pad(shell,24,10,24,20);shell.setBackground(bg(CANVAS,26));
        View handle=new View(this);handle.setBackground(bg(0xFFCDD5CB,3));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(38),dp(4));hp.gravity=Gravity.CENTER;hp.bottomMargin=dp(20);shell.addView(handle,hp);
        shell.addView(heading(name,23));if(!subtitle.isEmpty()){gap(shell,8);paragraph(shell,subtitle);}gap(shell,20);
        ScrollView sc=new ScrollView(this);sc.setFillViewport(false);sc.addView(bodyView);shell.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        dialog.setContentView(shell);Window w=dialog.getWindow();
        if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setDimAmount(.24f);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setGravity(Gravity.BOTTOM);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
        dialog.setOnShowListener(d->{if(w!=null)w.setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*.78f));if(motion()){shell.setTranslationY(dp(32));shell.setAlpha(.6f);shell.animate().translationY(0).alpha(1).setDuration(190).setInterpolator(new DecelerateInterpolator()).start();}});
        return dialog;
    }
    private void previewTemplate(TemplateCatalog.Template t){
        LinearLayout panel=col();LinearLayout art=col();art.setGravity(Gravity.CENTER);pad(art,20,18,20,18);art.setBackground(bg(t.color,20));
        ImageView preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setContentDescription("Template preview: "+t.name);art.addView(preview,new LinearLayout.LayoutParams(-1,dp(210)));templateThumb(t,preview);panel.addView(art);gap(panel,18);
        paragraph(panel,"Fully editable · Black & white · Saved on your phone");gap(panel,20);
        Dialog dialog=sheet(t.name,t.description,panel);LinearLayout actions=fixedSheetActions(panel);
        TextView save=button(favorites.contains(""+t.id)?"Saved ♥":"Save ♡",false,()->{if(!favorites.add(""+t.id))favorites.remove(""+t.id);saveDraft();dialog.dismiss();show();});actions.addView(save,new LinearLayout.LayoutParams(-2,dp(50)));
        LinearLayout.LayoutParams use=new LinearLayout.LayoutParams(0,dp(50),1);use.setMargins(dp(10),0,0,0);actions.addView(button("Make this yours",true,()->{dialog.dismiss();useTemplate(t.id);}),use);dialog.show();
    }
    private void useTemplate(int i){
        if(printer.busy()){message("You can edit after the current print finishes.");return;}
        remember();TemplateCatalog.Template t=TemplateCatalog.get(i);
        photoMode=false;brightness=0;typeSize=22;checklist=t.checklist;paperStyle=t.style;paperAlignment=t.style==2?1:0;title=t.name;body=t.body;
        editorOrigin=screen.equals("templates")?"templates":"home";screen="editor";saveDraft();show();renderPaper();notice("Template applied",this::undo);
    }
    private void editor(){
        LinearLayout bar=row();pad(bar,16,8,16,8);TextView back=button("‹",false,()->{saveDraft();screen=editorOrigin;show();});back.setContentDescription("Back to "+editorOrigin);bar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        editorTitle=heading(photoMode?"Your photo":"Your note",18);editorTitle.setGravity(Gravity.CENTER);bar.addView(editorTitle,new LinearLayout.LayoutParams(0,-2,1));
        undoAction=button("Undo",false,this::undo);undoAction.setContentDescription("Undo last edit");undoAction.setEnabled(!undoHistory.isEmpty());undoAction.setAlpha(undoHistory.isEmpty()?.4f:1);bar.addView(undoAction);headerHost.addView(bar);
        LinearLayout preview=col();preview.setGravity(Gravity.CENTER);pad(preview,18,14,18,18);preview.setBackground(bg(Color.rgb(236,238,229),20));
        TextView eyebrow=label("YOUR PAPER · LIVE PREVIEW",10,MUTED,true);eyebrow.setGravity(Gravity.CENTER);preview.addView(eyebrow);gap(preview,15);
        paperImage=new ImageView(this);paperImage.setScaleType(ImageView.ScaleType.FIT_CENTER);paperImage.setContentDescription("Actual monochrome print preview");
        int previewHeight=Math.max(176,Math.min(240,Math.round(getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density*.32f)));
        preview.addView(paperImage,new LinearLayout.LayoutParams(-1,dp(previewHeight)));paperImage.setOnClickListener(v->largePreview());paperImage.setFocusable(true);gap(preview,10);
        previewStatus=label("",12,GREEN,false);previewStatus.setGravity(Gravity.CENTER);preview.addView(previewStatus);
        paperInfo=label("",10,MUTED,false);paperInfo.setGravity(Gravity.CENTER);gap(preview,8);preview.addView(paperInfo);
        content.addView(preview,new LinearLayout.LayoutParams(-1,-2));gap(content,14);
        LinearLayout tools=row();
        if(photoMode){iconButton(tools,"photo","Replace",this::pickPhoto);iconButton(tools,"grid","Crop",this::cropPhoto);iconButton(tools,"style","Style",this::stylePhoto);iconButton(tools,"rotate","Rotate",()->{remember();turns=(turns+1)%4;renderPaper();});}
        else{iconButton(tools,"note","Text",this::editText);iconButton(tools,"grid","Layout",this::layoutNote);iconButton(tools,"style","Type size",this::fontSize);iconButton(tools,"heart","Templates",()->{screen="templates";show();});}
        content.addView(tools);gap(content,14);
        TextView tip=label(photoMode?"Tap the paper to see every detail.":"Tap the paper to zoom. All changes save automatically.",12,MUTED,false);tip.setGravity(Gravity.CENTER);content.addView(tip);gap(content,22);
        LinearLayout densityRow=row();densityRow.addView(label("Print density",12,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));
        String[] levels={"Light","Medium","Dark"};
        for(int i=1;i<=3;i++){final int level=i;TextView choice=label(levels[i-1],11,INK,i==density);pad(choice,10,13,10,13);choice.setMinHeight(dp(48));choice.setGravity(Gravity.CENTER);choice.setBackground(bg(i==density?MINT:CARD,22));feedback(choice,()->{if(printer.busy()){message("Density is fixed for the current print.");return;}if(density==level)return;remember();density=level;saveDraft();updateDensity();});choice.setSelected(i==density);densityChoices.add(choice);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);cp.setMargins(dp(5),0,0,0);densityRow.addView(choice,cp);}
        content.addView(densityRow);gap(content,18);jobPanel(content);
        LinearLayout footer=col();pad(footer,24,12,24,14);footer.setBackground(bg(CANVAS,0));
        footerInfo=label("",11,MUTED,false);footerInfo.setGravity(Gravity.CENTER);footer.addView(footerInfo);gap(footer,9);
        footerAction=button("",true,this::sendPrint);footer.addView(footerAction,new LinearLayout.LayoutParams(-1,dp(50)));footerHost.addView(footer);
        updatePreview();
    }
    private void updateDensity(){
        for(int i=0;i<densityChoices.size();i++){TextView t=densityChoices.get(i);boolean active=i+1==density;t.setBackground(bg(active?MINT:CARD,22));t.setSelected(active);t.setTypeface(Typeface.DEFAULT,active?Typeface.BOLD:Typeface.NORMAL);}
        if(undoAction!=null){undoAction.setEnabled(!undoHistory.isEmpty());undoAction.setAlpha(undoHistory.isEmpty()?.4f:1);}
    }
    private void updatePreview(){
        if(!screen.equals("editor")||paperImage==null)return;
        if(sheetPreview!=null&&paper!=null)sheetPreview.setImageBitmap(paper.bitmap);
        if(paper!=null){paperImage.setImageBitmap(paper.bitmap);paperImage.setAlpha(rendering?.65f:1f);}
        previewStatus.setText(rendering?"Updating your paper…":!renderError.isEmpty()?renderError:"Tap to take a closer look");
        paperInfo.setText(paper==null?"384 dots · Monochrome":paper.rows.length+" rows · True black & white");
        if(footerInfo!=null)footerInfo.setText(rendering?"Preparing preview…":!renderError.isEmpty()?"Adjust your content to continue":"Saved on this phone · "+(photoMode?"Photo":"Note"));
        if(footerAction!=null){footerAction.setText(printer.busy()?"View print progress":printer.ready?"Review & print":"Connect to print");footerAction.setEnabled(!rendering&&paper!=null);footerAction.setAlpha(rendering||paper==null?.5f:1f);}
        if(editorTitle!=null)editorTitle.setText(photoMode?"Your photo":"Your note");updateDensity();
    }
    private final class DraftState {
        final String oldTitle=title,oldBody=body;
        final boolean oldPhoto=photoMode,oldList=checklist,oldDither=dither;
        final int oldSize=typeSize,oldStyle=paperStyle,oldAlign=paperAlignment,oldCrop=crop,oldTurns=turns,oldBrightness=brightness,oldDensity=density;
        void restore(){title=oldTitle;body=oldBody;photoMode=oldPhoto;checklist=oldList;dither=oldDither;typeSize=oldSize;paperStyle=oldStyle;paperAlignment=oldAlign;crop=oldCrop;turns=oldTurns;brightness=oldBrightness;density=oldDensity;}
        boolean isCurrent(){return oldTitle.equals(title)&&oldBody.equals(body)&&oldPhoto==photoMode&&oldList==checklist&&oldDither==dither&&oldSize==typeSize&&oldStyle==paperStyle&&oldAlign==paperAlignment&&oldCrop==crop&&oldTurns==turns&&oldBrightness==brightness&&oldDensity==density;}
    }
    private void remember(){if(undoHistory.size()>=20)undoHistory.removeFirst();undoHistory.addLast(new DraftState());updateDensity();}
    private void undo(){
        if(printer.busy()){message("Let this print finish before changing the paper.");return;}
        if(undoHistory.isEmpty()){toast("You're at the beginning.");return;}
        boolean previousMode=photoMode;undoHistory.removeLast().restore();saveDraft();
        if(previousMode!=photoMode&&screen.equals("editor"))show();renderPaper();notice("Previous edit restored.",null);
    }
    private void notice(String value,Runnable action){
        noticeHost.removeAllViews();LinearLayout bar=row();pad(bar,18,10,12,10);bar.setBackground(bg(INK,14));
        TextView copy=label(value,12,Color.WHITE,false);bar.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        if(action!=null){TextView undo=label("Undo",13,MINT,true);pad(undo,14,12,10,12);undo.setMinHeight(dp(48));feedback(undo,action);bar.addView(undo);}
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-2);bp.setMargins(dp(16),dp(3),dp(16),dp(5));noticeHost.addView(bar,bp);
        ui.postDelayed(()->{if(bar.getParent()==noticeHost)noticeHost.removeView(bar);},4000);
    }
    private void largePreview(){
        if(paper==null)return;Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar);activePanel=dialog;LinearLayout layout=col();layout.setBackgroundColor(CARD);
        LinearLayout bar=row();pad(bar,18,12,18,12);bar.addView(heading("Your paper, up close.",18),new LinearLayout.LayoutParams(0,-2,1));bar.addView(button("Done",false,dialog::dismiss));layout.addView(bar);
        ZoomPaperView zoom=new ZoomPaperView(this,paper.bitmap);layout.addView(zoom,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=label("Pinch to zoom · Drag to explore · Double-tap to reset",12,MUTED,false);hint.setGravity(Gravity.CENTER);pad(hint,20,12,20,24);layout.addView(hint);dialog.setContentView(layout);dialog.show();
    }
    private void jobPanel(LinearLayout parent){
        stateText=label(printer.status,13,MUTED,false);stateText.setLineSpacing(dp(3),1);stateText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);parent.addView(stateText);
        progressBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progressBar.setProgressTintList(ColorStateList.valueOf(GREEN));parent.addView(progressBar,new LinearLayout.LayoutParams(-1,dp(8)));
        progressText=label("",11,MUTED,false);parent.addView(progressText);gap(parent,8);
        TextView help=label("Printing help  →",12,GREEN,true);pad(help,0,12,0,12);help.setMinHeight(dp(48));help.setOnClickListener(v->help());help.setFocusable(true);parent.addView(help);
    }
    private void editText(){
        DraftState before=new DraftState();LinearLayout fields=col();
        fields.addView(label("TITLE",10,MUTED,true));gap(fields,8);
        EditText titleInput=new EditText(this);titleInput.setText(title);titleInput.setHint("A title for your note");titleInput.setSingleLine(true);titleInput.setTextSize(17);titleInput.setBackground(bg(CARD,12));pad(titleInput,14,12,14,12);titleInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(50)});fields.addView(titleInput);gap(fields,18);
        fields.addView(label(checklist?"ONE ITEM PER LINE":"YOUR WORDS",10,MUTED,true));gap(fields,8);
        EditText contentInput=new EditText(this);contentInput.setText(body);contentInput.setHint("Make a little room for your words…");contentInput.setGravity(Gravity.TOP);contentInput.setMinLines(5);contentInput.setMaxLines(9);
        contentInput.setTextSize(16);contentInput.setBackground(bg(CARD,12));pad(contentInput,14,12,14,12);
        contentInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);contentInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1600)});fields.addView(contentInput);gap(fields,10);
        TextView count=label(body.length()+" / 1600 · Changes preview automatically",11,MUTED,false);fields.addView(count);gap(fields,20);
        Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar);activePanel=dialog;
        LinearLayout composer=col();composer.setBackgroundColor(CANVAS);LinearLayout bar=row();pad(bar,16,8,16,8);
        bar.addView(button("Cancel",false,()->{before.restore();dialog.dismiss();renderPaper();}));TextView caption=heading("Your words",18);caption.setGravity(Gravity.CENTER);bar.addView(caption,new LinearLayout.LayoutParams(0,-2,1));
        bar.addView(button("Done",true,()->{if(!before.isCurrent()){if(undoHistory.size()>=20)undoHistory.removeFirst();undoHistory.addLast(before);}dialog.dismiss();renderPaper();}));composer.addView(bar);
        ScrollView scroll=new ScrollView(this);pad(fields,24,18,24,24);scroll.addView(fields);composer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(composer);
        Window window=dialog.getWindow();if(window!=null){window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);window.setStatusBarColor(CANVAS);window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);}
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int n,int after){}public void onTextChanged(CharSequence s,int start,int before,int n){}
            public void afterTextChanged(Editable e){title=titleInput.getText().toString().trim();if(title.isEmpty())title="A little note.";body=contentInput.getText().toString();count.setText(getString(R.string.editor_count,body.length()));queueRender();}};
        titleInput.addTextChangedListener(watcher);contentInput.addTextChangedListener(watcher);
        dialog.setOnCancelListener(d->{before.restore();renderPaper();});dialog.show();if(window!=null)window.setLayout(-1,-1);
    }
    private void queueRender(){if(pendingRender!=null)ui.removeCallbacks(pendingRender);pendingRender=this::renderPaper;ui.postDelayed(pendingRender,180);}
    private void options(LinearLayout parent,String name,String[] choices,int selected,java.util.function.IntConsumer choose){
        parent.addView(label(name,11,MUTED,true));gap(parent,9);LinearLayout row=row();ArrayList<TextView> controls=new ArrayList<>();
        for(int i=0;i<choices.length;i++){final int value=i;TextView t=pill(choices[i],i==selected,()->{choose.accept(value);for(int j=0;j<controls.size();j++){TextView option=controls.get(j);option.setSelected(j==value);option.setTextColor(j==value?Color.WHITE:GREEN);option.setTypeface(Typeface.DEFAULT,j==value?Typeface.BOLD:Typeface.NORMAL);option.setBackground(new RippleDrawable(ColorStateList.valueOf(0x20305B49),bg(j==value?INK:CARD,23),bg(Color.WHITE,23)));}});pad(t,5,12,5,12);controls.add(t);LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(0,-2,1);if(i>0)op.setMargins(dp(7),0,0,0);row.addView(t,op);}
        parent.addView(row);gap(parent,20);
    }
    private void panelPreview(LinearLayout parent){
        sheetPreview=new ImageView(this);sheetPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);sheetPreview.setBackground(bg(CARD,16));pad(sheetPreview,16,12,16,12);sheetPreview.setContentDescription("Live paper preview");if(paper!=null)sheetPreview.setImageBitmap(paper.bitmap);parent.addView(sheetPreview,new LinearLayout.LayoutParams(-1,dp(150)));gap(parent,18);
    }
    private void sheetActions(LinearLayout fields,Dialog dialog,DraftState before){
        LinearLayout actions=fixedSheetActions(fields);
        TextView cancel=button("Cancel",false,()->{before.restore();dialog.dismiss();sheetPreview=null;renderPaper();});actions.addView(cancel,new LinearLayout.LayoutParams(-2,dp(50)));
        LinearLayout.LayoutParams keep=new LinearLayout.LayoutParams(0,dp(50),1);keep.setMargins(dp(10),0,0,0);
        actions.addView(button("Keep changes",true,()->{if(!before.isCurrent()){if(undoHistory.size()>=20)undoHistory.removeFirst();undoHistory.addLast(before);}dialog.dismiss();sheetPreview=null;renderPaper();}),keep);
        dialog.setOnCancelListener(d->{before.restore();sheetPreview=null;renderPaper();});dialog.setOnDismissListener(d->{sheetPreview=null;android.view.inputmethod.InputMethodManager im=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);im.hideSoftInputFromWindow(root.getWindowToken(),0);});
    }
    private LinearLayout fixedSheetActions(LinearLayout bodyView){
        LinearLayout shell=(LinearLayout)((View)bodyView.getParent()).getParent();LinearLayout actions=row();pad(actions,0,12,0,0);shell.addView(actions,new LinearLayout.LayoutParams(-1,-2));return actions;
    }
    private void layoutNote(){
        DraftState before=new DraftState();LinearLayout panel=col();panelPreview(panel);
        options(panel,"PAPER STYLE",new String[]{"Clean","Journal","Postcard"},paperStyle,i->{paperStyle=i;renderPaper();});
        options(panel,"CONTENT",new String[]{"Checklist","Simple note"},checklist?0:1,i->{checklist=i==0;renderPaper();});
        options(panel,"ALIGNMENT",new String[]{"Left","Center"},paperAlignment,i->{paperAlignment=i;renderPaper();});
        paragraph(panel,"Checklist items stay left-aligned for easier reading.");gap(panel,17);Dialog dialog=sheet("Find your paper's style.","A few thoughtful choices. A very different feeling.",panel);sheetActions(panel,dialog,before);dialog.show();
    }
    private void fontSize(){
        DraftState before=new DraftState();LinearLayout panel=col();panelPreview(panel);options(panel,"TYPE SIZE",new String[]{"Small","Medium","Large"},typeSize==16?0:typeSize==22?1:2,i->{typeSize=new int[]{16,22,28}[i];renderPaper();});
        paragraph(panel,"The preview shows the exact black & white output.");gap(panel,18);Dialog dialog=sheet("Room for your words.","Choose the size that feels right.",panel);sheetActions(panel,dialog,before);dialog.show();
    }
    private void cropPhoto(){
        DraftState before=new DraftState();LinearLayout panel=col();panelPreview(panel);options(panel,"FRAMING",new String[]{"Full image","Square","Portrait"},crop,i->{crop=i;renderPaper();});
        paragraph(panel,"Square and portrait use a centered crop. Full image keeps the whole frame.");gap(panel,18);Dialog dialog=sheet("A fresh perspective.","Find the frame before it finds the paper.",panel);sheetActions(panel,dialog,before);dialog.show();
    }
    private void stylePhoto(){
        DraftState before=new DraftState();LinearLayout fields=col();panelPreview(fields);
        Switch dithering=new Switch(this);dithering.setText(R.string.fine_detail);dithering.setChecked(dither);dithering.setMinHeight(dp(48));fields.addView(dithering);gap(fields,16);
        fields.addView(label("Brightness",14,INK,true));SeekBar seek=new SeekBar(this);seek.setMax(160);seek.setProgress(brightness+80);fields.addView(seek);
        dithering.setOnCheckedChangeListener((v,checked)->{dither=checked;renderPaper();});seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){renderPaper();}public void onProgressChanged(SeekBar s,int progress,boolean fromUser){if(fromUser){brightness=progress-80;queueRender();}}});
        gap(fields,18);Dialog dialog=sheet("Find your black & white.","Keep the detail. Set the mood.",fields);sheetActions(fields,dialog,before);dialog.show();
    }
    private void pickPhoto(){
        if(printer.busy()){message("Finish the current print before replacing your image.");return;}
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/*");
        try{startActivityForResult(intent,PICK_PHOTO);}catch(ActivityNotFoundException e){message("No photo picker is available on this device.");}
    }
    private void loadPhoto(Uri uri){
        undoHistory.clear();rendering=true;editorOrigin=screen.equals("templates")?"templates":"home";screen="editor";paper=null;show();int request=++renderVersion;
        worker.execute(()->{
            try{
                BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;
                try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,opts);}
                if(opts.outWidth<1||opts.outHeight<1)throw new IOException("Unsupported image format");
                opts.inSampleSize=1;while(Math.max(opts.outWidth,opts.outHeight)/opts.inSampleSize>2048)opts.inSampleSize*=2;
                opts.inJustDecodeBounds=false;Bitmap result;
                try(InputStream in=getContentResolver().openInputStream(uri)){result=BitmapFactory.decodeStream(in,null,opts);}
                if(result==null)throw new IOException("Could not read image");
                int orientation=1;
                try(InputStream in=getContentResolver().openInputStream(uri)){orientation=new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);}catch(IOException ignored){}
                Matrix m=new Matrix();switch(orientation){case 2:m.setScale(-1,1);break;case 3:m.setRotate(180);break;case 4:m.setScale(1,-1);break;case 5:m.setRotate(90);m.postScale(-1,1);break;case 6:m.setRotate(90);break;case 7:m.setRotate(270);m.postScale(-1,1);break;case 8:m.setRotate(270);break;}
                if(!m.isIdentity())result=Bitmap.createBitmap(result,0,0,result.getWidth(),result.getHeight(),m,true);
                File file=new File(getFilesDir(),"photo.png");try(OutputStream out=new FileOutputStream(file)){if(!result.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Could not save photo");}
                Bitmap chosen=result;ui.post(()->{if(destroyed||request!=renderVersion)return;photo=chosen;photoMode=true;crop=turns=brightness=0;saveDraft();show();renderPaper();});
            }catch(Exception|OutOfMemoryError e){ui.post(()->{if(destroyed||request!=renderVersion)return;rendering=false;photoMode=false;renderError="That photo could not be opened. Try a smaller JPG or PNG.";show();message(renderError);});}
        });
    }
    private void renderPaper(){
        if(pendingRender!=null)ui.removeCallbacks(pendingRender);
        saveDraft();final int request=++renderVersion;rendering=true;renderError="";
        final boolean isPhoto=photoMode, detail=dither, list=checklist;final Bitmap source=photo;
        final int c=crop,t=turns,b=brightness,size=typeSize,style=paperStyle,align=paperAlignment;final String titleCopy=title,bodyCopy=body;
        updatePreview();long began=android.os.SystemClock.elapsedRealtime();
        worker.execute(()->{
            if(destroyed||request!=renderVersion)return;
            try{
                Bitmap sourcePaper=isPhoto&&source!=null?PaperRenderer.photo(source,c,t):PaperRenderer.note(titleCopy,bodyCopy,size,list,style,align);
                PaperRenderer.Paper result=PaperRenderer.finish(sourcePaper,isPhoto&&detail,isPhoto?b:0);
                ui.post(()->{if(destroyed||request!=renderVersion)return;paper=result;rendering=false;latestRenderMs=android.os.SystemClock.elapsedRealtime()-began;if(BuildConfig.DEBUG)android.util.Log.d("FoliPreview","renderMs="+latestRenderMs);updatePreview();});
            }catch(Exception|OutOfMemoryError e){ui.post(()->{if(destroyed||request!=renderVersion)return;paper=null;rendering=false;renderError=e instanceof IllegalArgumentException?e.getMessage():"This paper could not be prepared. Try a smaller image or shorter note.";updatePreview();});}
        });
    }
    private void device(){
        deviceSnapshot=printer.ready+":"+printer.connecting+":"+printer.dirty+":"+printer.busy();
        content.addView(heading("Your FOLI",29));gap(content,10);paragraph(content,"A little connection. A world of little ideas.");gap(content,20);
        LinearLayout product=col();LinearLayout summary=row();summary.addView(new FoliIcon(this,"mark",GREEN),new LinearLayout.LayoutParams(dp(42),dp(42)));LinearLayout words=col();pad(words,16,0,0,0);
        deviceCopy=heading(printer.ready?"Connected via Bluetooth LE":printer.connecting?"Connecting…":"Let's find your FOLI.",17);words.addView(deviceCopy);gap(words,6);words.addView(label("Your paper companion",12,MUTED,false));summary.addView(words,new LinearLayout.LayoutParams(0,-2,1));product.addView(summary);gap(product,18);
        fullButton(product,printer.ready?"Refresh status":"Find your printer",true,()->{if(printer.ready)printer.refresh();else findPrinter();});card(content,product,MINT);gap(content,20);
        content.addView(label("REPORTED BY YOUR PRINTER",10,MUTED,true));gap(content,12);
        batteryValue=deviceStat("battery","Battery",printer.battery<0?"—":printer.battery+"%");
        paperValue=deviceStat("note","Paper",printer.paper<0?"Unknown":printer.paper==0?"Loaded":"Out of paper");
        temperatureValue=deviceStat("printer","Print head",PrinterProtocol.temperatureText(printer.temperature));gap(content,8);
        jobPanel(content);gap(content,10);
        if(printer.ready||printer.connecting)fullButton(content,"Disconnect",false,()->{if(printer.busy())confirmStop();else{printer.disconnect();show();}});
        if(printer.busy()){gap(content,12);fullButton(content,"Stop sending / disconnect",false,this::confirmStop);}
        if(printer.dirty){gap(content,15);fullButton(content,"I restarted the printer",false,()->new AlertDialog.Builder(this).setTitle("Clear the previous job?").setMessage("Turn the printer off, then on. This clears any partially received data. Confirm only after restarting it; reconnect if needed.").setNegativeButton("Not yet",null).setPositiveButton("Restarted",(d,w)->{printer.acknowledgeRestart();uncertainPrint=false;saveDraft();show();}).show());}
        gap(content,24);TextView privacy=label("Private by design · No account needed\nYour notes and photos stay on this phone.\nFOLI "+BuildConfig.VERSION_NAME+" · Development build",12,MUTED,false);privacy.setLineSpacing(dp(5),1);content.addView(privacy);
    }
    private TextView deviceStat(String ic,String name,String value){
        LinearLayout r=row();pad(r,16,15,16,15);r.setBackground(bg(CARD,15));r.addView(new FoliIcon(this,ic,GREEN),new LinearLayout.LayoutParams(dp(23),dp(23)));
        TextView key=label(name,13,MUTED,false);pad(key,15,0,0,0);r.addView(key,new LinearLayout.LayoutParams(0,-2,1));TextView readout=label(value,14,INK,true);r.addView(readout);content.addView(r);gap(content,10);return readout;
    }
    private boolean permissions(){
        String[] required=Build.VERSION.SDK_INT>=31?new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT}:new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        for(String p:required)if(checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED){requestPermissions(required,PERMISSIONS);return false;}
        return true;
    }
    private void findPrinter(){
        if(!permissions())return;
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return;
        BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
        if(adapter==null){message("This device does not support Bluetooth LE. You can still create and preview papers.");return;}
        if(!adapter.isEnabled()){try{startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),ENABLE_BT);}catch(Exception e){message("Turn on Bluetooth in your phone settings, then try again.");}return;}
        if(Build.VERSION.SDK_INT<31){LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);
            if(lm!=null&&!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)&&!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                new AlertDialog.Builder(this).setTitle("Enable location services").setMessage("Android 11 and earlier require location services for BLE discovery. FOLI does not collect your location.").setNegativeButton("Cancel",null).setPositiveButton("Open settings",(d,w)->startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))).show();return;
            }
        }
        scanDevices.clear();devicesAdapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>());
        LinearLayout scanPanel=col();pad(scanPanel,22,10,22,10);
        scanStatus=label("Power on your printer. Close other apps connected to it.\nLooking nearby…",14,MUTED,false);scanPanel.addView(scanStatus);
        ListView list=new ListView(this);list.setAdapter(devicesAdapter);scanPanel.addView(list,new LinearLayout.LayoutParams(-1,dp(240)));
        devicesDialog=new AlertDialog.Builder(this).setTitle("Find your FOLI").setView(scanPanel)
                .setNegativeButton("Close",null).setNeutralButton("Scan again",null).create();
        list.setOnItemClickListener((parent,view,index,id)->{if(index>=0&&index<scanDevices.size()){
            BluetoothDevice selected=scanDevices.get(index);devicesDialog.dismiss();printer.connect(selected);screen="device";show();
        }});
        devicesDialog.setOnDismissListener(d->printer.stopScan());devicesDialog.show();
        devicesDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->printer.scan());printer.scan();
    }
    @Override public void changed(){
        if(destroyed)return;
        if(printer.busy()||printer.dirty)uncertainPrint=true;
        else if(printer.jobOutcome==PrintOutcome.RESTART_CONFIRMED)uncertainPrint=false;
        saveDraft();
        if(devicesDialog!=null&&devicesDialog.isShowing()&&devicesAdapter!=null){
            scanDevices=new ArrayList<>(printer.devices.values());devicesAdapter.clear();
            for(BluetoothDevice d:scanDevices){String name;
                try{name=d.getName();devicesAdapter.add((name==null?"FOLI-compatible printer":name)+"\n"+d.getAddress());}catch(SecurityException ignored){}
            }
            devicesAdapter.notifyDataSetChanged();scanStatus.setText(getString(R.string.scan_status,printer.status));
        }
        String snapshot=printer.ready+":"+printer.connecting+":"+printer.dirty+":"+printer.busy();
        if(screen.equals("device")&&!snapshot.equals(deviceSnapshot))show();else updateStatus();
    }
    private void updateStatus(){
        if(batteryValue!=null)batteryValue.setText(printer.battery<0?"—":printer.battery+"%");
        if(paperValue!=null)paperValue.setText(printer.paper<0?"Unknown":printer.paper==0?"Loaded":"Out of paper");
        if(temperatureValue!=null)temperatureValue.setText(PrinterProtocol.temperatureText(printer.temperature));
        if(footerAction!=null)footerAction.setText(printer.busy()?"View print progress":printer.ready?"Review & print":"Connect to print");
        if(stateText!=null)stateText.setText(printer.status);
        if(chip!=null)chip.setText(connectionLabel());
        if(progressBar!=null){progressBar.setVisibility(printer.sending?View.VISIBLE:View.GONE);progressBar.setProgress(printer.progress);}
        if(progressText!=null){progressText.setText(printer.sending?printer.progress+"% sent · Keep FOLI open and nearby.":printer.awaiting?"Transfer accepted. Waiting for device confirmation.":"");progressText.setVisibility(printer.busy()?View.VISIBLE:View.GONE);}
        if(printer.busy())getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void sendPrint(){
        if(printer.busy()){new AlertDialog.Builder(this).setTitle("Your print").setMessage(printer.status).setNegativeButton("Keep going",null).setPositiveButton("Stop / disconnect",(d,w)->confirmStop()).show();return;}
        if(rendering){message("Your preview is still being prepared.");return;}
        if(paper==null){message(renderError.isEmpty()?"Create a paper first.":renderError);return;}
        if(!printer.ready){new AlertDialog.Builder(this).setTitle("Connect your FOLI").setMessage("Your paper is ready. Connect a compatible printer to send it.").setNegativeButton("Later",null).setPositiveButton("Find printer",(d,w)->findPrinter()).show();return;}
        String error=printer.canPrint();if(error!=null){message(error);return;}
        new AlertDialog.Builder(this).setTitle("Ready to print?").setMessage("Check the paper roll and keep the app open.\n\n"+paper.rows.length+" rows · "+new String[]{"Light","Medium","Dark"}[density-1]+" density\n\nThe printer may start as soon as the transfer finishes.")
                .setNegativeButton("Cancel",null).setPositiveButton("Print",(d,w)->{printer.print(paper.rows,density);updateStatus();}).show();
    }
    private void confirmStop(){new AlertDialog.Builder(this).setTitle("Stop this connection?").setMessage("FOLI can stop sending data, but the current firmware has no remote stop command. A print already started may continue. Restart the printer before retrying to clear its buffer.").setNegativeButton("Keep going",null).setPositiveButton("Stop sending",(d,w)->{printer.stopSending();printer.disconnect();show();}).show();}
    private void help(){new AlertDialog.Builder(this).setTitle("A little help").setMessage("1. Install the paper with its thermal side facing the print head.\n\n2. Power the printer from its battery; use a charged, compatible pack.\n\n3. Connect inside FOLI. Close the old printer app first.\n\n4. Keep FOLI visible during transfer. Interrupted transfers require a printer restart.\n\n5. If a print is sent but completion is unconfirmed, check the actual paper. Do not resend automatically.\n\n6. The legacy firmware may append a stray line and has limited completion/error reporting. Hardware verification is still required for this development build.").setPositiveButton("Got it",null).show();}
    @Override public void message(String value){if(!destroyed)new AlertDialog.Builder(this).setTitle("FOLI").setMessage(value).setPositiveButton("Got it",null).show();}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
    private void saveDraft(){
        if(printer==null)return;
        getPreferences(MODE_PRIVATE).edit().putString("title",title).putString("body",body).putBoolean("checklist",checklist).putBoolean("photoMode",photoMode)
                .putBoolean("dither",dither).putInt("brightness",brightness).putInt("density",density).putInt("typeSize",typeSize).putInt("crop",crop).putInt("turns",turns)
                .putInt("paperStyle",paperStyle).putInt("paperAlignment",paperAlignment).putStringSet("favorites",new HashSet<>(favorites))
                .putBoolean("uncertainPrint",uncertainPrint||printer.dirty||printer.busy()).apply();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==PERMISSIONS){boolean ok=grants.length>0;for(int g:grants)ok&=g==PackageManager.PERMISSION_GRANTED;
            if(ok)findPrinter();else new AlertDialog.Builder(this).setTitle("Nearby devices permission").setMessage("Allow Bluetooth access to discover and connect to your FOLI. Editing and preview work without it.").setNegativeButton("Not now",null).setPositiveButton("App settings",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==PICK_PHOTO&&result==RESULT_OK&&data!=null&&data.getData()!=null)loadPhoto(data.getData());
        if(request==ENABLE_BT&&result==RESULT_OK)findPrinter();
    }
    @Override public void onBackPressed(){if(screen.equals("editor")){screen=editorOrigin;show();}else if(!screen.equals("home")){screen="home";show();}else if(printer.busy())confirmStop();else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle out){out.putString("screen",screen);out.putString("editorOrigin",editorOrigin);out.putString("filter",templateFilter);if(pageScroll!=null)scrollPositions.put(screen,pageScroll.getScrollY());for(Map.Entry<String,Integer> entry:scrollPositions.entrySet())out.putInt("scroll_"+entry.getKey(),entry.getValue());super.onSaveInstanceState(out);saveDraft();}
    @Override protected void onPause(){saveDraft();super.onPause();}
    @Override protected void onStop(){
        printer.stopScan();if(printer.busy()){printer.stopSending();saveDraft();}super.onStop();
    }
    @Override protected void onDestroy(){destroyed=true;renderVersion++;if(activePanel!=null&&activePanel.isShowing())activePanel.dismiss();if(devicesDialog!=null&&devicesDialog.isShowing())devicesDialog.dismiss();printer.close();saveDraft();worker.shutdownNow();thumbWorker.shutdownNow();ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
