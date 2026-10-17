package ar.deltagreen.indice;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.zip.*;

/** Biblioteca personal: el APK no contiene los PDF, audios ni ayudas de terceros. */
public class MainActivity extends Activity {
    private static final String HOST="il.local";
    private static final int BOOK_PICK=7, PACK_PICK=8, ITEM_PICK=9, JSON_EXPORT=71, JSON_IMPORT=72;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private WebView web;
    private LinearLayout layout, reader;
    private FrameLayout content;
    private TextView status;
    private EditText pageInput;
    private ZoomImage pageImage;
    private PdfRenderer renderer;
    private ParcelFileDescriptor descriptor;
    private File renderedFile;
    private Bitmap renderedBitmap;
    private int currentPage=1,pendingPage=1;
    private boolean reading=false,importing=false;
    private String pendingItem="",exportJson="",readerName="Impossible Landscapes";

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    private File bookFile(){return new File(getFilesDir(),"impossible-370.pdf");}
    private File mediaDir(){File f=new File(getFilesDir(),"evidencias");if(!f.exists())f.mkdirs();return f;}
    private String extFor(String mime){
        if(mime==null)return "";
        mime=mime.toLowerCase(Locale.ROOT);
        if(mime.contains("jpeg")||mime.contains("jpg"))return "jpg";
        if(mime.contains("png"))return "png";
        if(mime.contains("webp"))return "webp";
        if(mime.contains("pdf"))return "pdf";
        if(mime.contains("mpeg")||mime.contains("mp3"))return "mp3";
        if(mime.contains("mp4"))return "mp4";
        return "";
    }
    private String mimeFor(String ext){
        switch(ext){case "jpg":return "image/jpeg";case "png":return "image/png";case "webp":return "image/webp";case "pdf":return "application/pdf";case "mp3":return "audio/mpeg";case "mp4":return "video/mp4";default:return "application/octet-stream";}
    }
    private boolean safeId(String id){return id!=null&&id.matches("[a-z0-9_-]{1,64}");}
    private File installedMedia(String id){
        if(!safeId(id))return null;
        String ext=getPreferences(0).getString("m_"+id,"");
        if(extFor(mimeFor(ext)).isEmpty())return null;
        File f=new File(mediaDir(),id+"."+ext);
        return f.isFile()?f:null;
    }
    private Button button(String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setTextSize(13);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;
    }
    private void notifyUser(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.rgb(10,19,20));
        layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setBackgroundColor(Color.rgb(10,19,20));setContentView(layout);
        layout.setOnApplyWindowInsetsListener((v,i)->{layout.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
        LinearLayout navTop=new LinearLayout(this);
        navTop.addView(button("Archivo",()->showIndex()),new LinearLayout.LayoutParams(0,dp(49),1));
        navTop.addView(button("Manual PDF",()->openPage(currentPage)),new LinearLayout.LayoutParams(0,dp(49),1));
        navTop.addView(button("Importar libro",()->pickBook()),new LinearLayout.LayoutParams(0,dp(49),1));
        navTop.addView(button("Ayudas ZIP",()->pickPack()),new LinearLayout.LayoutParams(0,dp(49),1));
        layout.addView(navTop);
        status=new TextView(this);status.setTextSize(12);status.setTextColor(Color.rgb(224,195,120));status.setPadding(dp(12),dp(4),dp(12),dp(5));
        layout.addView(status);
        content=new FrameLayout(this);layout.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        web=new WebView(this);web.setBackgroundColor(Color.rgb(14,22,26));
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new JsBridge(),"Android");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){
                Uri uri=req.getUrl();return !"https".equals(uri.getScheme())||!HOST.equals(uri.getHost());
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return serve(r);}
        });
        content.addView(web,new FrameLayout.LayoutParams(-1,-1));
        web.loadUrl("https://"+HOST+"/assets/index.html");
        reader=new LinearLayout(this);reader.setOrientation(LinearLayout.VERTICAL);reader.setBackgroundColor(Color.rgb(8,12,15));reader.setVisibility(View.GONE);
        content.addView(reader,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout readerNav=new LinearLayout(this);
        readerNav.addView(button("‹",()->displayCurrent(currentPage-1)),new LinearLayout.LayoutParams(dp(55),dp(48)));
        pageInput=new EditText(this);pageInput.setInputType(2);pageInput.setTextSize(16);pageInput.setSingleLine(true);pageInput.setSelectAllOnFocus(true);
        readerNav.addView(pageInput,new LinearLayout.LayoutParams(0,dp(48),1));
        readerNav.addView(button("Ir",()->{try{displayCurrent(Integer.parseInt(pageInput.getText().toString()));}catch(Exception e){notifyUser("Ingresá un número de página.");}}));
        readerNav.addView(button("›",()->displayCurrent(currentPage+1)),new LinearLayout.LayoutParams(dp(55),dp(48)));
        reader.addView(readerNav);
        pageImage=new ZoomImage();reader.addView(pageImage,new LinearLayout.LayoutParams(-1,0,1));
        currentPage=getPreferences(0).getInt("lastPage",1);
        status.setText(bookFile().exists()?"Manual disponible sin conexión · Biblioteca personal":"Importá el libro original de 370 páginas · Después, las ayudas privadas");
    }
    private WebResourceResponse serve(WebResourceRequest r){
        Uri u=r.getUrl();if(!"https".equals(u.getScheme())||!HOST.equals(u.getHost()))return null;
        String p=u.getPath();if(p==null)return null;
        try{
            if(p.startsWith("/assets/")){
                String name=p.substring(8);
                if(!name.matches("[a-zA-Z0-9_.-]{1,90}"))return null;
                String mime=name.endsWith(".js")?"application/javascript":name.endsWith(".png")?"image/png":"text/html";
                return new WebResourceResponse(mime,mime.startsWith("text/")||mime.contains("javascript")?"UTF-8":null,getAssets().open(name));
            }
            if(p.startsWith("/media/")){
                String id=p.substring(7);File f=installedMedia(id);if(f==null)return null;
                String ext=getPreferences(0).getString("m_"+id,"");
                String range=r.getRequestHeaders().get("Range");
                long start=0,end=f.length()-1;
                if(range!=null&&range.matches("bytes=\\d+-\\d*")){
                    String[] seg=range.substring(6).split("-",-1);start=Long.parseLong(seg[0]);
                    if(!seg[1].isEmpty())end=Math.min(end,Long.parseLong(seg[1]));
                    if(start> end)return null;
                }
                FileInputStream source=new FileInputStream(f);long skipped=0;
                while(skipped<start){long x=source.skip(start-skipped);if(x<=0){source.close();return null;}skipped+=x;}
                final long byteCount=end-start+1;
                InputStream limited=new FilterInputStream(source){long remaining=byteCount;
                    @Override public int read()throws IOException{if(remaining<=0)return -1;int b=super.read();if(b>=0)remaining--;return b;}
                    @Override public int read(byte[] b,int off,int len)throws IOException{if(remaining<=0)return -1;int n=super.read(b,off,(int)Math.min(len,remaining));if(n>0)remaining-=n;return n;}
                };
                Map<String,String> headers=new HashMap<>();headers.put("Accept-Ranges","bytes");headers.put("Content-Length",String.valueOf(end-start+1));
                int code=200;String reason="OK";
                if(range!=null&&range.matches("bytes=\\d+-\\d*")){code=206;reason="Partial Content";headers.put("Content-Range","bytes "+start+"-"+end+"/"+f.length());}
                return new WebResourceResponse(mimeFor(ext),null,code,reason,headers,limited);
            }
        }catch(Exception ignored){}
        return null;
    }

    private class JsBridge {
        @JavascriptInterface public void openPage(int n){ui.post(()->MainActivity.this.openPage(n));}
        @JavascriptInterface public void openDocument(String id){ui.post(()->openMediaDocument(id));}
        @JavascriptInterface public boolean hasMedia(String id){return installedMedia(id)!=null;}
        @JavascriptInterface public String mediaUrl(String id){return installedMedia(id)!=null?"https://"+HOST+"/media/"+id:"";}
        @JavascriptInterface public void importOne(String id){if(!safeId(id))return;ui.post(()->pickOne(id));}
        @JavascriptInterface public void requestPack(){ui.post(()->pickPack());}
        @JavascriptInterface public void exportCampaign(String data){if(data==null||data.length()>900000)return;ui.post(()->{exportJson=data;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"impossible-landscapes-respaldo.json");startActivityForResult(i,JSON_EXPORT);});}
        @JavascriptInterface public void importCampaign(){ui.post(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,JSON_IMPORT);});}
    }

    private void showIndex(){reading=false;reader.setVisibility(View.GONE);web.setVisibility(View.VISIBLE);}
    private void pickBook(){if(importing)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");startActivityForResult(i,BOOK_PICK);}
    private void pickPack(){if(importing)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/zip","application/x-zip-compressed","application/octet-stream"});startActivityForResult(i,PACK_PICK);}
    private void pickOne(String id){if(importing)return;pendingItem=id;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/png","image/jpeg","image/webp","application/pdf","audio/mpeg","video/mp4"});startActivityForResult(i,ITEM_PICK);}
    private void openPdf(File file,String name)throws Exception{
        if(renderer!=null&&file.equals(renderedFile))return;
        if(renderer!=null){renderer.close();renderer=null;}
        if(descriptor!=null){descriptor.close();descriptor=null;}
        descriptor=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
        renderer=new PdfRenderer(descriptor);renderedFile=file;readerName=name;
    }
    private void openMediaDocument(String id){
        File f=installedMedia(id);if(f==null){notifyUser("Primero importá este documento.");return;}
        if(!f.getName().endsWith(".pdf")){notifyUser("Este recurso se abre en la galería.");return;}
        try{openPdf(f,"Ayuda: "+id);displayCurrent(1);}catch(Exception e){notifyUser("No se pudo abrir el PDF.");}
    }
    private void openPage(int n){
        pendingPage=n;File f=bookFile();if(!f.isFile()){notifyUser("Importá una vez el manual original de 370 páginas.");pickBook();return;}
        try{openPdf(f,"Impossible Landscapes");displayCurrent(n);}catch(Exception e){notifyUser("No se pudo abrir el manual.");}
    }
    private void displayCurrent(int n){
        if(importing){notifyUser("Esperá a que termine la importación.");return;}
        if(renderer==null){openPage(n);return;}
        if(n<1||n>renderer.getPageCount()){notifyUser("Página fuera del documento.");return;}
        currentPage=n;if(renderedFile.equals(bookFile()))getPreferences(0).edit().putInt("lastPage",n).apply();
        reading=true;web.setVisibility(View.GONE);reader.setVisibility(View.VISIBLE);pageInput.setText(String.valueOf(n));pageInput.clearFocus();
        try(PdfRenderer.Page p=renderer.openPage(n-1)){
            int width=Math.min(2000,Math.max(1000,getResources().getDisplayMetrics().widthPixels*2));
            int height=(int)((long)width*p.getHeight()/p.getWidth());
            Bitmap next=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            next.eraseColor(Color.WHITE);p.render(next,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pageImage.setImageBitmap(next);pageImage.reset();Bitmap old=renderedBitmap;renderedBitmap=next;
            if(old!=null&&old!=next)old.recycle();
            status.setText(readerName+" · "+n+"/"+renderer.getPageCount()+" · Separá dos dedos para ampliar");
        }catch(Exception e){notifyUser("No se pudo mostrar esta página.");}
    }
    @Override public void onBackPressed(){if(reading)showIndex();else new AlertDialog.Builder(this).setMessage("¿Cerrar Impossible Landscapes?").setPositiveButton("Cerrar",(d,w)->finish()).setNegativeButton("Seguir",null).show();}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();if(renderer!=null)renderer.close();if(descriptor!=null)try{descriptor.close();}catch(Exception ignored){}if(web!=null)web.destroy();}

    @Override protected void onActivityResult(int req,int result,Intent intent){
        super.onActivityResult(req,result,intent);
        if(result!=RESULT_OK||intent==null||intent.getData()==null)return;
        Uri uri=intent.getData();
        if(req==JSON_EXPORT){try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(exportJson.getBytes(StandardCharsets.UTF_8));notifyUser("Respaldo guardado.");}catch(Exception e){notifyUser("Error al exportar.");}return;}
        if(req==JSON_IMPORT){try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>900000)throw new IOException("Demasiado grande");out.write(buf,0,n);}
            String payload=out.toString("UTF-8");web.evaluateJavascript("window.restoreCampaign("+org.json.JSONObject.quote(payload)+")",null);
        }catch(Exception e){notifyUser("Respaldo no válido.");}return;}
        if(req==BOOK_PICK)importBook(uri);
        if(req==PACK_PICK)importPack(uri);
        if(req==ITEM_PICK)importItem(uri,pendingItem);
    }
    private void callbackImported(int amount,String msg){
        importing=false;status.setText(msg);notifyUser(msg);
        web.evaluateJavascript("window.ilMediaImported && window.ilMediaImported("+amount+")",null);
    }
    private void importBook(Uri uri){
        if(importing)return;importing=true;status.setText("Copiando PDF original a memoria privada…");
        io.execute(()->{
            File tmp=new File(getFilesDir(),"import-manual.tmp");
            try{
                copyLimited(uri,tmp,180L*1024*1024);
                int pages;try(ParcelFileDescriptor d=ParcelFileDescriptor.open(tmp,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer check=new PdfRenderer(d)){pages=check.getPageCount();}
                if(pages!=370)throw new IOException("El manual debe tener 370 páginas (archivo elegido: "+pages+").");
                ui.post(()->{
                    try{
                        if(renderer!=null){renderer.close();renderer=null;}
                        if(descriptor!=null){descriptor.close();descriptor=null;}
                        File dest=bookFile();if(dest.exists()&&!dest.delete())throw new IOException("No se pudo reemplazar el PDF");
                        if(!tmp.renameTo(dest))throw new IOException("No se pudo guardar el manual");
                        importing=false;openPage(Math.max(1,Math.min(pendingPage,370)));
                    }catch(Exception e){callbackImported(0,"Error: "+e.getMessage());}
                });
            }catch(Exception e){tmp.delete();ui.post(()->callbackImported(0,"Error al importar el manual: "+e.getMessage()));}
        });
    }
    private void copyLimited(Uri uri,File target,long max)throws IOException{
        try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(target)){
            if(in==null)throw new IOException("Archivo inaccesible");
            byte[] b=new byte[32768];int n;long sum=0;
            while((n=in.read(b))!=-1){sum+=n;if(sum>max)throw new IOException("Archivo demasiado grande");out.write(b,0,n);}
        }
    }
    private void importItem(Uri uri,String id){
        if(importing||!safeId(id))return;
        String ext=extFor(getContentResolver().getType(uri));
        if(ext.isEmpty()){
            try(Cursor c=getContentResolver().query(uri,null,null,null,null)){
                if(c!=null&&c.moveToFirst()){
                    int col=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if(col>=0){String name=c.getString(col).toLowerCase(Locale.ROOT);int dot=name.lastIndexOf('.');if(dot>=0){String candidate=name.substring(dot+1);if(candidate.equals("jpeg"))candidate="jpg";if(!extFor(mimeFor(candidate)).isEmpty())ext=candidate;}}
                }
            }catch(Exception ignored){}
        }
        if(ext.isEmpty()){notifyUser("Tipo de archivo no compatible. Usá PDF, JPG, PNG, WEBP, MP3 o MP4.");return;}
        final String useExt=ext;importing=true;status.setText("Importando ayuda "+id+"…");
        io.execute(()->{
            File tmp=new File(mediaDir(),"__import.tmp");
            try{
                copyLimited(uri,tmp,100L*1024*1024);
                File finalFile=new File(mediaDir(),id+"."+useExt);
                if(finalFile.exists()&&!finalFile.delete())throw new IOException("No se pudo reemplazar");
                if(!tmp.renameTo(finalFile))throw new IOException("No se pudo guardar");
                getPreferences(0).edit().putString("m_"+id,useExt).apply();
                ui.post(()->callbackImported(1,"Ayuda importada: "+id));
            }catch(Exception e){tmp.delete();ui.post(()->callbackImported(0,"No se pudo importar: "+e.getMessage()));}
        });
    }
    private void importPack(Uri uri){
        if(importing)return;importing=true;status.setText("Importando paquete privado de ayudas…");
        io.execute(()->{
            int count=0;long total=0;
            try(InputStream input=getContentResolver().openInputStream(uri);ZipInputStream zip=new ZipInputStream(new BufferedInputStream(input))){
                ZipEntry entry;byte[] buf=new byte[32768];
                while((entry=zip.getNextEntry())!=null){
                    String name=entry.getName();Matcher m=Pattern.compile("^media/([a-z0-9_-]{1,64})\\.(jpg|png|webp|pdf|mp3|mp4)$").matcher(name);
                    if(entry.isDirectory()||!m.matches()){zip.closeEntry();continue;}
                    String id=m.group(1),ext=m.group(2);
                    File tmp=new File(mediaDir(),"__zip.tmp");long one=0;
                    try(OutputStream out=new FileOutputStream(tmp)){
                        int n;while((n=zip.read(buf))!=-1){one+=n;total+=n;
                            if(one>110L*1024*1024||total>350L*1024*1024)throw new IOException("Paquete demasiado grande");
                            out.write(buf,0,n);
                        }
                    }
                    File dest=new File(mediaDir(),id+"."+ext);
                    if(dest.exists()&&!dest.delete())throw new IOException("No se pudo reemplazar "+id);
                    if(!tmp.renameTo(dest))throw new IOException("No se pudo guardar "+id);
                    getPreferences(0).edit().putString("m_"+id,ext).apply();count++;
                    zip.closeEntry();
                }
                final int installed=count;ui.post(()->callbackImported(installed,"Paquete importado · "+installed+" recursos disponibles sin conexión"));
            }catch(Exception e){ui.post(()->callbackImported(0,"Error en el ZIP: "+e.getMessage()+". Los recursos ya importados siguen disponibles."));}
        });
    }
    class ZoomImage extends ImageView {
        final Matrix matrix=new Matrix();float lastX,lastY,scale=1;
        final ScaleGestureDetector zoom;
        ZoomImage(){super(MainActivity.this);setScaleType(ScaleType.MATRIX);setBackgroundColor(Color.rgb(13,19,21));
            zoom=new ScaleGestureDetector(MainActivity.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
                @Override public boolean onScale(ScaleGestureDetector d){float factor=d.getScaleFactor();float target=Math.max(1,Math.min(5,scale*factor));factor=target/scale;scale=target;matrix.postScale(factor,factor,d.getFocusX(),d.getFocusY());setImageMatrix(matrix);return true;}
            });
        }
        void reset(){scale=1;post(this::fit);}
        void fit(){if(getDrawable()==null||getWidth()==0||getHeight()==0)return;matrix.setRectToRect(new RectF(0,0,getDrawable().getIntrinsicWidth(),getDrawable().getIntrinsicHeight()),new RectF(0,0,getWidth(),getHeight()),Matrix.ScaleToFit.CENTER);setImageMatrix(matrix);scale=1;}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);fit();}
        @Override public boolean onTouchEvent(MotionEvent ev){zoom.onTouchEvent(ev);
            if(ev.getActionMasked()==MotionEvent.ACTION_MOVE&&!zoom.isInProgress()&&scale>1){matrix.postTranslate(ev.getX()-lastX,ev.getY()-lastY);setImageMatrix(matrix);}
            lastX=ev.getX();lastY=ev.getY();return true;
        }
    }
}
