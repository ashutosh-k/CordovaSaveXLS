package CordovaSaveXLS;


import android.util.Log;
 
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.widget.Toast;

import android.Manifest;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.Intent;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import jxl.*;
import jxl.format.Alignment;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;


public class CordovaSaveXLS extends CordovaPlugin {
    public static final String ACTION_SAVE_XLS = "saveXLS";
    // Storage Permissions
    public int rowPosition = 1;
    public Boolean hasTitles = false;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
     
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
 
        try {
            if(ACTION_SAVE_XLS.equals(action)){
                /*  Modelo dos parâmetros
                    var data = [
                                    {id:"1", name:"claudio"} ,
                                    {id:"2", name:"marta"} ,
                                    {id:"3", name:"isabela"} 
                               ];
                    dirname = "ExcelAPI";
                    filename = "file-example.xls";
                    sheetname = "Plan1";
                */
                
                JSONObject params = args.getJSONObject(0);

                try{
		   //Define o nome do diretório
                    this.dirname = params.getString("dirname");
                    String fileName = params.getString("filename");
                    File localFile = this.getFilePath(fileName);
                    localFile.delete();
                    
                    //Define o nome do arquivo
                    WritableWorkbook wb = this.createWorkbook(fileName);

                    //Define o nome da planilha
                    WritableSheet sheetObject = this.createSheet(wb, params.getString("sheetname"), 1);
                    
                    
                    JSONArray lineItems = params.getJSONArray("data");
                    
                    //Loop pelas linhas de dados
                    this.rowPosition = 1;
                    this.hasTitles = false;
                    for (int i = 0, size = lineItems.length(); i < size; i++){
                        JSONObject objectInArray = lineItems.getJSONObject(i);
                        jsonObjectToCell(sheetObject, objectInArray);
                    }
                    
                    //Escrevendo os dados no arquivo
                    wb.write();
                    
                    //Fechando o arquivo
                    wb.close();

                    
                    android.app.DownloadManager downloadManager = (android.app.DownloadManager) cordova.getActivity().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    downloadManager.addCompletedDownload(localFile.getName(), localFile.getName(), true, "application/vnd.ms-excel", localFile.getAbsolutePath(),localFile.length(),true);
                    //Precisa adicionar esse callback para informar ao phonegap que
                    //a execução ocorreu com sucesso
                    callbackContext.success(localFile.getAbsolutePath());
                    return true;
                }
                catch(IOException ex){
                    Log.e(TAG, ex.getStackTrace().toString());
                    Log.e(TAG, ex.getMessage());
                    callbackContext.error(ex.getMessage());
                }
            }
           
            callbackContext.error("Invalid action");
            return false;
        }
        catch(Exception e) {
            System.err.println("Exception: " + e.getMessage());
            callbackContext.error(e.getMessage());
            Log.v(TAG, e.getStackTrace().toString());
            return false;
        } 
    }     
    
    public String dirname;
    
   /**
     * @property int rowPosition
     * Inicia a contagem das linhas em 1, para que a linha 0 seja 
     */

    
   /**
     * @property Boolean hasTitles
     * Armazena a informação se a planilha já tem títulos definidos
     */
    
    
   /**
     * @param  sheetObj - Sheet object
     * @para   obj      - JSON object
     * @return void
     */
    public void jsonObjectToCell(WritableSheet sheetObj, JSONObject obj){
    	try{
    		//Inicia a contagem das colunas em 0
    		int columnPosition = 0;
            
            Iterator<?> keys = obj.keys();
	        while( keys.hasNext() ){
	        	//Pega o nome da chave
	        	String key = (String)keys.next();
	        	
                //Pega o valor da chave
	            String value = obj.getString(key);
	            
	            try{
	            	//Se não tem título definido
		        	if(!hasTitles){
		        		//Escreve os títulos na primeira linha
		        		this.writeCell(columnPosition, 0, key, true, sheetObj);
		        	}
		        	
                    //Escreve os valores dos objetos na célula a partir da segunda linha (rowPosition=1)
	        		this.writeCell(columnPosition, rowPosition, value, false, sheetObj);
		            
	        		Log.i(TAG, key +":"+ value);
		            
                    //Adiciona 1 ao contador de colunas
		            columnPosition++;
	            }
	            catch(WriteException we){
	            	Log.i(TAG, we.toString());
	            }
	        }
            this.rowPosition++;
            this.hasTitles = true;

    	}
    	catch(JSONException e){
    		Log.i(TAG, e.toString());
    	}
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
  /**
    * @param fileName - the name to give the new workbook file
    * @return - a new WritableWorkbook with the given fileName 
    */
    public File getFilePath(String fileName) {
        this.verifyStoragePermissions(cordova.getActivity());

        //get the sdcard's directory
        File sdCard = null;
        if(this.isExternalStorageWritable()){
            sdCard = Environment.getExternalStorageDirectory();
        } else {
            sdCard = cordova.getActivity().getApplicationContext().getFilesDir();
        }
        //add on the your app's path
        File dir = new File(sdCard.getAbsolutePath() + File.separator + Environment.DIRECTORY_DOWNLOADS);
        //make them in case they're not there
        //dir.mkdirs();
        //create a standard java.io.File object for the Workbook to use
        return new File(dir, fileName);
    }
    public WritableWorkbook createWorkbook(String fileName) throws IOException {
        //exports must use a temp file while writing to avoid memory hogging
        WorkbookSettings wbSettings = new WorkbookSettings(); 				
        //wbSettings.setUseTemporaryFileDuringWrite(true);
        File wbfile = this.getFilePath(fileName);
     
        WritableWorkbook wb = null;
     
        try{
        //create a new WritableWorkbook using the java.io.File and
        //WorkbookSettings from above
            wb = Workbook.createWorkbook(wbfile,wbSettings);
        }catch(IOException ex){
            Log.e(TAG,ex.getStackTrace().toString());
            Log.e(TAG, ex.getMessage());
            throw ex;
        }
     
        return wb;	                
    }
 
    String TAG = "xls";
    
  /**
    * @param wb - WritableWorkbook to create new sheet in
    * @param sheetName - name to be given to new sheet
    * @param sheetIndex - position in sheet tabs at bottom of workbook
    * @return - a new WritableSheet in given WritableWorkbook
    */
    public WritableSheet createSheet(WritableWorkbook wb, String sheetName, int sheetIndex){
       //create a new WritableSheet and return it
       return wb.createSheet(sheetName, sheetIndex);
    }

   /**
     * @param columnPosition - column to place new cell in
     * @param rowPosition - row to place new cell in
     * @param contents - string value to place in cell
     * @param headerCell - whether to give this cell special formatting
     * @param sheet - WritableSheet to place cell in
     * @throws RowsExceededException - thrown if adding cell exceeds .xls row limit
     * @throws WriteException - Idunno, might be thrown
     */
    public void writeCell(int columnPosition, int rowPosition, String contents, boolean headerCell,
        WritableSheet sheet) throws RowsExceededException, WriteException{
        //create a new cell with contents at position
        Label newCell = new Label(columnPosition,rowPosition,contents);
     
        if (headerCell){
            //give header cells size 10 Arial bolded 	
            WritableFont headerFont = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
            WritableCellFormat headerFormat = new WritableCellFormat(headerFont);
            //center align the cells' contents
            headerFormat.setAlignment(Alignment.CENTRE);
            newCell.setCellFormat(headerFormat);
        }
     
        sheet.addCell(newCell);
    }
}
