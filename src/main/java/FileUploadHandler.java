

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.oreilly.servlet.MultipartRequest;

/**
 * Servlet implementation class FileUploadHandler
 */
@MultipartConfig
public class FileUploadHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String SAVE_DIR = "New";
	private static final String CONVERTED_FILE_DIR = "Converted";
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileUploadHandler() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		String fileName = "";
		String fileExtension = "";
		String applicationPath = request.getServletContext().getRealPath("");
		String savePath = applicationPath + File.separator + SAVE_DIR;
		String convertedFilePath = applicationPath + File.separator + CONVERTED_FILE_DIR;
		
		System.out.println("convertedFilePath--"+convertedFilePath);
		
		// creates the save directory if it does not exists
        File fileSaveDir = new File(savePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdir();
        }
        
        // creates the converted file save directory if it does not exists
        File convfileSaveDir = new File(convertedFilePath);
        if (!convfileSaveDir.exists()) {
        	convfileSaveDir.mkdir();
        }
		
		MultipartRequest m = new MultipartRequest(request, savePath);
		Enumeration files = m.getFileNames(); 
		while (files.hasMoreElements()) { 
			   String name =(String)files.nextElement();
		       fileName = m.getFilesystemName(name);
		}
		
		fileExtension = fileName.substring(fileName.lastIndexOf(".")+1);
		System.out.println("fileName--"+fileName+"--"+fileExtension);
		
		File f = new File(savePath + File.separator + fileName);
		 
		if("xlsx".equalsIgnoreCase(fileExtension) || ("xls".equalsIgnoreCase(fileExtension))){
			
			FileInputStream fis = new FileInputStream(f);
			byte[] bytesArray = new byte[(int) f.length()]; 
			fis.read(bytesArray); //read file into bytes[]
		    fis.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(bytesArray);
			Workbook wb = new XSSFWorkbook(bais);
			
			DataFormatter formatter = new DataFormatter();
			for (int i=0 ; i<wb.getNumberOfSheets(); i++) {
				Sheet sheet = wb.getSheetAt(i);
				String sheetName = wb.getSheetName(i);
				PrintStream out = new PrintStream(new FileOutputStream(new File(convertedFilePath + File.separator + sheetName + ".csv")), true, "UTF-8");
				for (int r = 0, rn = sheet.getLastRowNum() ; r <= rn ; r++) {
			        Row row = sheet.getRow(r);
			        boolean firstCell = true;
			        for (int c = 0, cn = row.getLastCellNum() ; c < cn ; c++) {
			            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			            if ( ! firstCell ) out.print(',');
			            String text = formatter.formatCellValue(cell);
			            out.print(text);
			            firstCell = false;
			        }
			        out.println();
			    }
				out.close();
			}
			wb.close();
			//request.setAttribute("message", "File has been converted successfully!");
		}else{
			request.setAttribute("message", "File type not supported!");
		}
		
		//delete file from temporary savepath
		f.delete();
		
		//insert csv files in SF
		String result = FileInsert.processFileAndInsert(convertedFilePath + File.separator);
		
		if("Success".equalsIgnoreCase(result)){
			request.setAttribute("message", "File has been converted and saved in SF successfully!");
		}else{
			request.setAttribute("message", "Error occured.Please contact Administartor.");
		}
		System.out.println(request.getAttribute("message"));
		
		//redirect the response to result.jsp page
		request.getServletContext().getRequestDispatcher("/result.jsp").forward(request, response);
	
	}

}
