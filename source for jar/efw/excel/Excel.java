/**** efw3.X Copyright 2016 efwGrp ****/
package efw.excel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaRenderer;
import org.apache.poi.ss.formula.FormulaRenderingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.AreaPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.poi.xssf.usermodel.XSSFTextParagraph;
import org.apache.poi.xssf.usermodel.XSSFTextRun;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import efw.efwException;
import efw.file.FileManager;
/**
 * Excelオブジェクトを取り扱うクラス。
 * @author Chang Kejun
 *
 */
public final class Excel {
	/**
	 * Excelのパス。
	 */
	private File file;
	/**
	 * ExcelのPOIオブジェクト。
	 */
	private Workbook workbook;
	/**
	 * コンストラクタ
	 * @param path　Excelの相対パス。
	 * @param workbook　ExcelのPOIオブジェクト。
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 * @throws EncryptedDocumentException 
	 */
	protected Excel(File file) throws EncryptedDocumentException, InvalidFormatException, IOException {
		//一時ファイルを作成する。
		//引数のfileを一時ファイルにコピーする。
		//一時ファイルでexcelを開く。
		//閉じる際、一時ファイルを削除する。
		File tempFile=File.createTempFile("efw", "");
		FileManager.duplicate(file, tempFile);
		this.file=tempFile;
		this.workbook = WorkbookFactory.create(tempFile);
	}
	/**
	 * ExcelのPOIオブジェクトを削除する。
	 * @throws IOException
	 */
	public void close() throws IOException {
		try {
			workbook.close();
			this.file.delete();
		} catch (IOException e) {
			throw e;
		}
	}
	/**
	 * 指定シートの利用されている最大行番号を戻る。0から開始。
	 * @param sheetName　シート名
	 * @return　最大行番号　0から開始
	 */
	public int getMaxRow(String sheetName){
		return this.workbook.getSheet(sheetName).getLastRowNum();
	}
	/**
	 * 指定シートの利用されている最大列番号を戻る。0から開始。
	 * @param sheetName　シート名
	 * @return　最大列番号　0から開始
	 */
	public int getMaxCol(String sheetName){
		return this.workbook.getSheet(sheetName).getLastRowNum();
	}
	/**
	 * セルを取得する。
	 * @param sheetName　シート名
	 * @param position セルの位置　"A0" のように表現する。
	 * @return セルオブジェクトを戻す
	 */
	private Cell getCell(String sheetName, String position){
		Sheet sheet =this.workbook.getSheet(sheetName);
		CellReference reference = new CellReference(position);
        Row row = this.workbook.getSheet(sheetName).getRow(reference.getRow());
        Cell cell = null;
        if (row == null) {
        	row = sheet.createRow(reference.getRow());
       	}
        cell = row.getCell(reference.getCol());
        if (cell==null){
        	cell=row.createCell(reference.getCol());
        }
        return cell;
	}
	
	/**
	 * セルの値を取得する。
	 * @param sheetName　シート名
	 * @param position セルの位置　"A0" のように表現する。
	 * @return
	 */
	public Object get(String sheetName, String position) {
		try {
			Cell cell=getCell(sheetName,position);
			FormulaEvaluator evaluator = workbook.getCreationHelper()
					.createFormulaEvaluator();
			switch (cell.getCellTypeEnum()) {
			case BOOLEAN:
				return cell.getBooleanCellValue();
			case ERROR:
				return cell.getErrorCellValue();
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return cell.getDateCellValue();
				} else {
					return cell.getNumericCellValue();
				}
			case STRING:
				return cell.getStringCellValue();
			case FORMULA:
				CellValue cellValue = evaluator.evaluate(cell);
				switch (cellValue.getCellTypeEnum()) {
				case BOOLEAN:
					return cell.getBooleanCellValue();
				case ERROR:
					return cell.getErrorCellValue();
				case NUMERIC:
					if (DateUtil.isCellDateFormatted(cell)) {
						return cell.getDateCellValue();
					} else {
						return cell.getNumericCellValue();
					}
				case STRING:
					return cell.getStringCellValue();
				case _NONE:
				case BLANK:
				default:
					break;
				}
			case _NONE:
			case BLANK:
			}
		} catch (Exception e) {
		}
		return null;
	}
	/**
	 * シートの印刷範囲を設定。
	 * @param sheetName　シート名。
	 * @param startRow　開始行番号。
	 * @param endRow　終了行番号。
	 * @param startCol 開始列番号。
	 * @param endCol 終了列番号。
	 */
	public void setPrintArea(String sheetName, int startRow, int endRow, int startCol, int endCol){
		Sheet sheet = workbook.getSheet(sheetName);
		workbook.setPrintArea(workbook.getSheetIndex(sheet), startCol, endCol, startRow, endRow);
	}
	/**
	 * シートを作成する。
	 * @param sheetName　シート名。
	 * @param templateSheetName　コピー元シート名。
	 */
	public void createSheet(String sheetName,String templateSheetName){
		if (templateSheetName==null){
			workbook.createSheet(sheetName);
		}else{
			Sheet sheet = workbook.cloneSheet(workbook.getSheetIndex(templateSheetName));
			workbook.setSheetName(workbook.getSheetIndex(sheet.getSheetName()), sheetName);
			Sheet tempSheet = workbook.getSheet(templateSheetName);
			PrintSetup tempPrintSetup = tempSheet.getPrintSetup();
			PrintSetup printSetup = sheet.getPrintSetup();
			//印刷へーダー
			sheet.setRepeatingRows(tempSheet.getRepeatingRows());
			//印刷用紙設定
            printSetup.setPaperSize(tempPrintSetup.getPaperSize());
			//印刷方向
            printSetup.setLandscape(tempPrintSetup.getLandscape());
			//ページ設定の拡大縮小印刷
            printSetup.setFitHeight(tempPrintSetup.getFitHeight());
            printSetup.setFitWidth(tempPrintSetup.getFitWidth());
		}
	}
	/**
	 * シートを削除する。
	 * @param sheetName　シート名。
	 */
	public void removeSheet(String sheetName){
		workbook.removeSheetAt(workbook.getSheetIndex(sheetName));
	}
	/**
	 * シート名の配列を取得する。
	 * @return　シート名の配列
	 */
	public ArrayList<String> getSheetNames(){
		ArrayList<String> allSheetNames = new ArrayList<String>();
        Iterator<Sheet> it = workbook.sheetIterator();

        while (it.hasNext()) {
            Sheet sheet = it.next();
            allSheetNames.add(sheet.getSheetName());
        }
        return allSheetNames;		
	}
	/**
	 * セルにリンクを追加する
	 * @param sheetName リンクを追加するセルが所属するシート名
	 * @param position リンクを追加するセル名
	 * @param linkUrl リンクした先名 "#'シート名'!A1"のように
	 */
	public void setLink(String sheetName,String position,String linkUrl){
		
		CreationHelper ch = workbook.getCreationHelper();
		Hyperlink link = null;
		if(linkUrl.startsWith("mailto:")){
			link = ch.createHyperlink(HyperlinkType.EMAIL);
		}else if(linkUrl.startsWith("http:") || linkUrl.startsWith("https:")){
			link = ch.createHyperlink(HyperlinkType.URL);
		}else if(linkUrl.startsWith("#'") && linkUrl.indexOf("'!")!=(-1)){
			link = ch.createHyperlink(HyperlinkType.DOCUMENT);
		}else{
			link = ch.createHyperlink(HyperlinkType.FILE);
		}
		link.setAddress(linkUrl);
		Cell cell=this.getCell(sheetName, position);
		cell.setHyperlink(link);
	}
	/**
	 * シートの順番を設定する。
	 * @param sheetName　シート名。
	 * @param order　順番。　0　から。
	 */
	public void setSheetOrder(String sheetName,int order){
		workbook.setSheetOrder(sheetName,order);
	}
	/**
	 * シートをActive設定する。
	 * @param sheetName　シート名。
	 */
	public void setActiveSheet(String sheetName){
        Iterator<Sheet> it = workbook.sheetIterator();
        while (it.hasNext()) {
            Sheet sheet = it.next();
            if (sheet.isSelected()) {
                sheet.setSelected(false);
            }
        }
		workbook.setActiveSheet(workbook.getSheetIndex(sheetName));
	}
	/**
	 * 保存する。
	 * @param path 保存先のパスファイル名。storageからの相対パス。
	 */
	public void save(String path){
		File fileNewExcel = FileManager.get(path);
		File filePath=new File(fileNewExcel.getParent());
		if (!filePath.exists()) {
			filePath.mkdirs();
		}
		FileOutputStream out = null;
        try {
        	ArrayList<String> nms=this.getSheetNames();
        	for(int i=0;i<nms.size();i++){
        		this.workbook.getSheet(nms.get(i)).setForceFormulaRecalculation(true);
        	}
            out = new FileOutputStream(fileNewExcel);
            workbook.write(out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                System.out.println(e.toString());
            }
        }		
	}
	/**
	 * セルに数字の値を設定する
	 * @param sheetName シート名。
	 * @param position セルの場所、"A1"のように。
	 * @param value 値。
	 */
	public void setCellDoubleValue(String sheetName, String position, Double value){
		Cell cell=this.getCell(sheetName, position);
		cell.setCellValue(value);
	}
	/**
	 * セルに日付の値を設定する
	 * @param sheetName シート名。
	 * @param position セルの場所、"A1"のように。
	 * @param value 値。
	 */
	public void setCellDateValue(String sheetName, String position, Date value){
		Cell cell=this.getCell(sheetName, position);
		cell.setCellValue(value);
	}
	/**
	 * セルに文字列の値を設定する
	 * @param sheetName シート名。
	 * @param position セルの場所、"A1"のように。
	 * @param value 値。
	 */
	public void setCellStringValue(String sheetName, String position, String value){
		Cell cell=this.getCell(sheetName, position);
		cell.setCellValue(value);
	}
	/**
	 * セルにブルの値を設定する
	 * @param sheetName シート名。
	 * @param position セルの場所、"A1"のように。
	 * @param value 値。
	 */
	public void setCellBooleanValue(String sheetName, String position, Boolean value){
		Cell cell=this.getCell(sheetName, position);
		cell.setCellValue(value);
	}
	///////////////////////////////////
	/**
	 * セルに書式を設定する。
	 * @param sheetName シート名。
	 * @param position セルの場所、"A1"のように。
	 * @param templateSheetName　参考するシート名。
	 * @param templatePosition　参考するセルの場所。
	 */
	public void setCellStyle(String sheetName, String position, String templateSheetName, String templatePosition){
		Cell cell=this.getCell(sheetName, position);
		Cell templateCell=this.getCell(templateSheetName, templatePosition);
		cell.setCellStyle(templateCell.getCellStyle());
	}
	
	/**
	 * セルに入力規則を設定する。
	 * @param sheetName シート名。
	 * @param position セルの場所、"A1"のように。
	 * @param templateSheetName　参考するシート名。
	 * @param templatePosition　参考するセルの場所。
	 */
	public void setCellValidations(String sheetName, String position, String templateSheetName, String templatePosition){
		//Validation
        Sheet fromSheet = this.workbook.getSheet(templateSheetName);
        CellReference fromPositionReference = new CellReference(templatePosition);
        List<? extends DataValidation> dataValidations = fromSheet.getDataValidations();
        List<DataValidation> addDataValidations = new ArrayList<DataValidation>();
        for (DataValidation dataValidation: dataValidations) {
            CellRangeAddressList region = dataValidation.getRegions();
            for (CellRangeAddress address : region.getCellRangeAddresses()) {
                if (address.containsRow(fromPositionReference.getRow()) && address.containsColumn(fromPositionReference.getCol())) {
                    addDataValidations.add(dataValidation);
                }
            }
        }
        Sheet sheet = this.workbook.getSheet(sheetName);
        CellReference positionReference = new CellReference(position);
        DataValidationHelper helper = sheet.getDataValidationHelper();
        CellRangeAddressList list = new CellRangeAddressList();
        list.addCellRangeAddress(positionReference.getRow(), positionReference.getCol(), positionReference.getRow(), positionReference.getCol());
        for (DataValidation dataValidation: addDataValidations) {
            sheet.addValidationData(helper.createValidation(dataValidation.getValidationConstraint(), list));
        }
	}
	/**
	 * セルに計算式を設定する。
	 * @param sheetName シート名。
	 * @param position セルの場所、"A1"のように。
	 * @param templateSheetName　参考するシート名。
	 * @param templatePosition　参考するセルの場所。
	 */
	
	public void setCellFormula(String sheetName, String position, String templateSheetName, String templatePosition){
		//Style
		Cell cell=this.getCell(sheetName, position);
		Cell templateCell=this.getCell(templateSheetName, templatePosition);
		
		if(templateCell.getCellTypeEnum()==CellType.FORMULA){
			String formula = templateCell.getCellFormula();
	        EvaluationWorkbook ew;
	        FormulaRenderingWorkbook rw;
	        Ptg[] ptgs;
	        if (this.workbook instanceof HSSFWorkbook) {
	            ew = HSSFEvaluationWorkbook.create((HSSFWorkbook) this.workbook);
	            ptgs = FormulaParser.parse(formula, (HSSFEvaluationWorkbook) ew, FormulaType.CELL, this.workbook.getSheetIndex(sheetName));
	            rw = (HSSFEvaluationWorkbook) ew;
	        } else {
	            ew = XSSFEvaluationWorkbook.create((XSSFWorkbook) this.workbook);
	            ptgs = FormulaParser.parse(formula, (XSSFEvaluationWorkbook) ew, FormulaType.CELL, this.workbook.getSheetIndex(sheetName));
	            rw = (XSSFEvaluationWorkbook) ew;
	        }
	        for (Ptg ptg : ptgs) {
	            // 座標の計算
	            int shiftRows = cell.getRowIndex() - templateCell.getRowIndex();
	            int shiftCols = cell.getColumnIndex() - templateCell.getColumnIndex();
	            if (ptg instanceof RefPtgBase) {
	                RefPtgBase ref = (RefPtgBase) ptg;
	                if (ref.isColRelative()) {
	                    ref.setColumn(ref.getColumn() + shiftCols);
	                }
	                if (ref.isRowRelative()) {
	                    ref.setRow(ref.getRow() + shiftRows);
	                }
	            } else if (ptg instanceof AreaPtg) {
	                AreaPtg ref = (AreaPtg) ptg;
	                if (ref.isFirstColRelative()) {
	                    ref.setFirstColumn(ref.getFirstColumn() + shiftCols);
	                }
	                if (ref.isLastColRelative()) {
	                    ref.setLastColumn(ref.getLastColumn() + shiftCols);
	                }
	                if (ref.isFirstRowRelative()) {
	                    ref.setFirstRow(ref.getFirstRow() + shiftRows);
	                }
	                if (ref.isLastRowRelative()) {
	                    ref.setLastRow(ref.getLastRow() + shiftRows);
	                }
	            }
	        }
	        cell.setCellFormula(FormulaRenderer.toFormulaString(rw, ptgs));
	        //Set all sheets, when closing.
	        //this.workbook.getSheet(sheetName).setForceFormulaRecalculation(true);
		}
	}
	

	
	/**
	 * 囲まれるかどうかを判断する。
	 * @param sheetName シート名。
	 * @param position セルの場所、"A1"のように。
	 * @param checkpointXRate 囲まれるエリアの中央点がセルの幅と比較する割合。0.5は中央。
	 * @param checkpointYRate 囲まれるエリアの中央点がセルの高さと比較する割合。0.5は中央。 
	 * @return
	 */
	public boolean isEncircled(String sheetName,String position,double checkpointXRate,double checkpointYRate){
		Cell cell=this.getCell(sheetName, position);
		int cellrow=cell.getRowIndex();
		int cellcol=cell.getColumnIndex();

		XSSFSheet sheet = ((XSSFWorkbook) (this.workbook)).getSheet(sheetName);
		XSSFDrawing patriarch = sheet.getDrawingPatriarch();
		if(patriarch==null) return false;
    	int checkPointX =(int)(checkpointXRate * cell.getSheet().getColumnWidth(cell.getColumnIndex()) / 256 * 8 * XSSFShape.EMU_PER_PIXEL);
    	int checkPointY = (int)(checkpointYRate * cell.getRow().getHeight() / 20.0D * XSSFShape.EMU_PER_POINT);
        List<XSSFShape> shapes=patriarch.getShapes();
        for (XSSFShape shape : shapes) {
            XSSFClientAnchor a = (XSSFClientAnchor)shape.getAnchor();
            if (checkEncircled(cellrow,cellcol,checkPointX,checkPointY,a.getRow1(),a.getCol1(),a.getRow2(),a.getCol2(),a.getDx1(),a.getDy1(),a.getDx2(),a.getDy2())) return true;
        }

        return false;
	}
	/**
	 * isEncircledに利用する内部関数。囲まれたかどうか判断する。
	 * @param cellrow セルの行番号
	 * @param cellcol　セルの列番号
	 * @param offsetYInEMU 囲まれるエリアの中央点横座標(EMU)
	 * @param offsetXInEMU　囲まれるエリアの中央点縦座標(EMU)
	 * @param row1 shape開始セルの行番号
	 * @param col1 shape開始セルの列番号
	 * @param row2 shape終了セルの行番号
	 * @param col2 shape開始セルの列番号
	 * @param dx1 shape開始セル内、shapeの左上位置の横座標(EMU)
	 * @param dy1 shape開始セル内、shapeの左上位置の縦座標(EMU)
	 * @param dx2 shape終了セル内、shapeの左上位置の横座標(EMU)
	 * @param dy2 shape終了セル内、shapeの左上位置の縦座標(EMU)
	 * @return　true囲まれる false囲まれない
	 */
	private boolean checkEncircled(
			int cellrow,int cellcol,int checkPointX,int checkPointY,
			int row1,int col1,int row2,int col2, int dx1,int dy1,int dx2,int dy2
		){
        boolean rowFlag=false;
        boolean colFlag=false;
        if (row1<cellrow && cellrow<row2){
        	rowFlag=true;
        }else if(row1==cellrow && row2==cellrow){
        	if(dy1<=checkPointY && checkPointY<=dy2)rowFlag=true;
        }else if(row1==cellrow){
        	if(dy1<=checkPointY)rowFlag=true;
        }else if(row2==cellrow){
        	if(checkPointY<=dy2)rowFlag=true;
        }
        if (col1<cellcol && cellcol<col2){
        	colFlag=true;
        }else if(col1==cellcol && col2==cellcol){
        	if(dx1<=checkPointX && checkPointX<=dx2)colFlag=true;
        }else if(col1==cellcol){
        	if(dx1<=checkPointX)colFlag=true;
        }else if(col2==cellcol){
        	if(checkPointX<=dx2)colFlag=true;
        }
        if (rowFlag&&colFlag){
            return true;
        }else{
            return false;
        }
	}
	/**
	 * 指定sheetの指定セルの指定位置に、図形をコピーして置く。
	 * @param sheetName シート名。
	 * @param position セルの場所、"A1"のように。
	 * @param templateSheetName　参考するシート名。
	 * @param templateShapeName　参考する図形の名称。
	 * @param shapeCenterXRate　新しい図形の中心位置はセルの幅との比率、デフォルト0.5。
	 * @param shapeCenterYRate　新しい図形の中心位置はセルの高さとの比率、デフォルト0.5。　
	 * @param shapeWidthRate　新しい図形の幅はセルの幅との比率、デフォルト0.5。
	 * @param shapeHeightRate　新しい図形の幅はセルの高さとの比率、デフォルト0.5。
	 */
	public void encircle(String sheetName,String position,String templateSheetName,String templateShapeName,
			double shapeCenterXRate,double shapeCenterYRate,double shapeWidthRate,double shapeHeightRate){
		Cell cell=this.getCell(sheetName, position);
	    int cellrow=cell.getRowIndex();
	    int cellcol=cell.getColumnIndex();

    	XSSFSheet sheet = (XSSFSheet) this.workbook.getSheet(sheetName);
    	XSSFSheet templateSheet=(XSSFSheet) this.workbook.getSheet(templateSheetName);
    	List<XSSFShape> templateShapes=((XSSFDrawing) templateSheet.getDrawingPatriarch()).getShapes();
        for (XSSFShape templateShape : templateShapes) {
    		if (templateShape instanceof XSSFSimpleShape && 
    				templateShapeName.equals(Excel.getShapeName((XSSFSimpleShape) templateShape))) {
                XSSFDrawing patriarch=sheet.getDrawingPatriarch();
    			if(patriarch==null) patriarch = sheet.createDrawingPatriarch();
    			XSSFClientAnchor anchor=(XSSFClientAnchor)(Excel.cloneShape(patriarch,(XSSFSimpleShape) templateShape).getAnchor());

    			int cellWidth=(int)(cell.getSheet().getColumnWidth(cell.getColumnIndex()) / 256 * 8 * XSSFShape.EMU_PER_PIXEL);
    		    int cellHeight=(int)(cell.getRow().getHeight() / 20.0D * XSSFShape.EMU_PER_POINT);
    		    double shapeWidth=cellWidth*shapeWidthRate;
    		    double shapeHeight=cellHeight*shapeHeightRate;
    		    
    			int dx1= (int)(cellWidth*shapeCenterXRate-shapeWidth/2);
    			int dx2= (int)(dx1+shapeWidth);
    			int dy1= (int)(cellHeight*shapeCenterYRate-shapeHeight/2);
    			int dy2= (int)(dy1+shapeHeight);
    			
    			anchor.setRow1(cellrow);
    			anchor.setRow2(cellrow);
    			anchor.setCol1(cellcol);
    			anchor.setCol2(cellcol);
    			anchor.setDx1(dx1);
    			anchor.setDx2(dx2);
    			anchor.setDy1(dy1);
    			anchor.setDy2(dy2);

    			break;
			}
        }
	}
	
	/**
	 * 指定sheetの指定セルの指定位置に、図形をコピーして置く。
	 * @param sheetName シート名。
	 * @param position セルの場所、"A1"のように。
	 * @param templateSheetName　参考するシート名。
	 * @param templateShapeName　参考する図形の名称。
	 * @param value 新しい図形の内容。
	 * @param x 新しい図形の左上位置はセルの幅との長さ、デフォルトが参考するシートの図形と同じ。
	 * @param y 新しい図形の左上位置はセルの高さとの高さ、デフォルトが参考するシートの図形と同じ。　
	 * @param width 新しい図形の長さ。
	 * @param height 新しい図形の高さ。
	 */
	public void addShapeInCell(String sheetName,String position,String templateSheetName,String templateShapeName,
			String value,int x,int y,int width,int height){
		CellReference reference = new CellReference(position);
	    int cellrow=reference.getRow();
	    int cellcol=reference.getCol();

    	XSSFSheet sheet = (XSSFSheet) this.workbook.getSheet(sheetName);
    	XSSFSheet templateSheet=(XSSFSheet) this.workbook.getSheet(templateSheetName);
    	List<XSSFShape> templateShapes=((XSSFDrawing) templateSheet.getDrawingPatriarch()).getShapes();
        for (XSSFShape templateShape : templateShapes) {
    		if (templateShape instanceof XSSFSimpleShape && 
    				templateShapeName.equals(Excel.getShapeName((XSSFSimpleShape) templateShape))) {
                XSSFDrawing patriarch=sheet.getDrawingPatriarch();
    			if(patriarch==null) patriarch = sheet.createDrawingPatriarch();
    			XSSFSimpleShape shape=Excel.cloneShape(patriarch,(XSSFSimpleShape) templateShape);
    			// 図形の中に文字内容を設定する
    			if(value!=null){
					if(shape.getTextParagraphs().size()>0){
						XSSFTextParagraph paragraph=shape.getTextParagraphs().get(0);
						if(paragraph.getTextRuns().size()>0){
							paragraph.getTextRuns().get(0).setText(value);
						}
					}
				}
    			XSSFClientAnchor anchor=(XSSFClientAnchor)(shape.getAnchor());
    			int dx1=XSSFShape.EMU_PER_PIXEL*x;
    			int dy1=XSSFShape.EMU_PER_PIXEL*y;
    			int dx2=0;
    			int dy2=0;
    			width=width*XSSFShape.EMU_PER_PIXEL;
    			height=height*XSSFShape.EMU_PER_PIXEL;
    			if(x==0){
    				dx1=anchor.getDx1();
    			}
    			if(y==0){
    				dy1=anchor.getDy1();
    			}
    			if(width==0){
    				width=anchor.getDx2()-anchor.getDx1();
    			}
    			if(height==0){
    				height=anchor.getDy2()-anchor.getDy1();
    			}
    			dx2=width+dx1;
    			dy2=height+dy1;
    			anchor.setRow1(cellrow);
    			anchor.setRow2(cellrow);
    			anchor.setCol1(cellcol);
    			anchor.setCol2(cellcol);
    			anchor.setDx1(dx1);
    			anchor.setDy1(dy1);
    			anchor.setDx2(dx2);
    			anchor.setDy2(dy2);
    			break;
			}
        }
	}
	
	/**
	 * 指定sheetの指定セルの指定位置に、図形をコピーして置く。
	 * @param sheetName シート名。
	 * @param firstCellPosition セルの場所、"A1"のように。
	 * @param lastCellPosition セルの場所、"A1"のように。
	 * @param templateSheetName　参考するシート名。
	 * @param templateShapeName　参考する図形の名称。
	 * @param value 新しい図形の内容。
	 * @param dx1 新しい図形の左上位置はセルの幅との長さ、デフォルトが参考するシートの図形と同じ。
	 * @param dy1 新しい図形の左上位置はセルの高さとの高さ、デフォルトが参考するシートの図形と同じ。
	 * @param dx2 新しい図形の右下位置はセルの幅との長さ、デフォルトが参考するシートの図形と同じ。
	 * @param dy2 新しい図形の右下位置はセルの高さとの高さ、デフォルトが参考するシートの図形と同じ。
	 */
	public void addShapeInRange(String sheetName,String firstCellPosition,String lastCellPosition,String templateSheetName,String templateShapeName,
			String value,int dx1,int dy1,int dx2,int dy2){
		CellReference reference = new CellReference(firstCellPosition);
	    int firstCellrow=reference.getRow();
	    int firstCellcol=reference.getCol();
		reference = new CellReference(lastCellPosition);
	    int lastCellrow=reference.getRow();
	    int lastCellcol=reference.getCol();

    	XSSFSheet sheet = (XSSFSheet) this.workbook.getSheet(sheetName);
    	XSSFSheet templateSheet=(XSSFSheet) this.workbook.getSheet(templateSheetName);
    	List<XSSFShape> templateShapes=((XSSFDrawing) templateSheet.getDrawingPatriarch()).getShapes();
        for (XSSFShape templateShape : templateShapes) {
    		if (templateShape instanceof XSSFSimpleShape && 
    				templateShapeName.equals(Excel.getShapeName((XSSFSimpleShape) templateShape))) {
                XSSFDrawing patriarch=sheet.getDrawingPatriarch();
    			if(patriarch==null) patriarch = sheet.createDrawingPatriarch();
    			
    			XSSFSimpleShape shape=Excel.cloneShape(patriarch,(XSSFSimpleShape) templateShape);
    			// 図形の中に文字内容を設定する
    			if(value!=null){
					if(shape.getTextParagraphs().size()>0){
						XSSFTextParagraph paragraph=shape.getTextParagraphs().get(0);
						if(paragraph.getTextRuns().size()>0){
							paragraph.getTextRuns().get(0).setText(value);
						}
					}
				}
    			XSSFClientAnchor anchor=(XSSFClientAnchor)(shape.getAnchor());
    			anchor.setRow1(firstCellrow);
    			anchor.setRow2(lastCellrow);
    			anchor.setCol1(firstCellcol);
    			anchor.setCol2(lastCellcol);
    			dx1=dx1*XSSFShape.EMU_PER_PIXEL;
    			dy1=dy1*XSSFShape.EMU_PER_PIXEL;
    			dx2=dx2*XSSFShape.EMU_PER_PIXEL;
    			dy2=dy2*XSSFShape.EMU_PER_PIXEL;
    			if(dx1==0){
    				dx1=anchor.getDx1();
    			}
    			if(dy1==0){
    				dy1=anchor.getDy1();
    			}
    			if(dx2==0){
    				dx2=anchor.getDx2();
    			}
    			if(dy2==0){
    				dy2=anchor.getDy2();
    			}
    			anchor.setDx1(dx1);
    			anchor.setDy1(dy1);
    			anchor.setDx2(dx2);
    			anchor.setDy2(dy2);
    			break;
			}
        }
	}	
	
	/**
	 * 行を追加
	 * @param sheetName シート名
	 * @param startRow このインデックスの上に、行を追加 
	 * @param n 追加する行数
	 * @return
	 */
	public void addRow(String sheetName,int startRow,int n){
		Sheet sheet = this.workbook.getSheet(sheetName);
		if(startRow <= sheet.getLastRowNum()) {
		    sheet.shiftRows(startRow, sheet.getLastRowNum(), n);
		}
	}
	
	/**
	 * 行を削除 インデックスは 「0」から 「endRow」の行を含めない
	 * @param sheetName シート名
	 * @param startRow 削除する行の開始インデックス from 0
	 * @param n 削除する行数
	 * @return
	 */
	public void delRow(String sheetName,int startRow,int n){
		Sheet sheet = this.workbook.getSheet(sheetName);
		for(int i=0;i<n;i++) {
			sheet.removeRow(sheet.getRow(startRow+i));
		}
		sheet.shiftRows(startRow+n, sheet.getLastRowNum(), n);
	}
	
	
	/**
	 * 列を追加
	 * @param sheetName シート名
	 * @param startCol このインデックスの上に、列を追加から
	 * @param n 追加する列数
	 * @return
	 */
	/*public void addCol(String sheetName,int startCol,int n){
		Sheet sheet = this.workbook.getSheet(sheetName);
		Row row = null;
		for(int i=0;i<=sheet.getLastRowNum();i++){
			row = sheet.getRow(i);
			if (this.workbook instanceof HSSFWorkbook) {
				HSSFRow hrow=(HSSFRow)row;
			}else{
				XSSFRow xrow=(XSSFRow)row;
				
			}
			for(int j=row.getLastCellNum()-1;j>=startCol;j--) {
				copyCell(workbook, row.getCell(j), row.createCell(j + n), true);
				row.removeCell(row.getCell(j));
			}
		}


		XSSFRow r;
		Field x=XSSFRow.class.getField("_cells");
		x.setAccessible(true);
		TreeMap _cells=x.get(r);
		
		
		r.createCell(x0)
		XSSFRowShifter a;
		
		a.shiftMergedRegions(startRow, endRow, n)
		
	}*/
	
	/**
	 * 列を削除 インデックスは 「0」から
	 * @param sheetName シート名
	 * @param startCol 削除する列の開始インデックス
	 * @param endCol 削除する列の結束インデックス
	 * @return
	 */
	/*public void delCol(String sheetName,int startCol,int endCol) throws efwException{
		Sheet sheet = this.workbook.getSheet(sheetName);
		Row row = null;
		int cellNum = 0;
		int newCellNum = 0;
		for(int i=0;i<=sheet.getLastRowNum();i++){
			row = sheet.getRow(i);
			cellNum = (int)row.getLastCellNum();
			newCellNum = row.getLastCellNum() - endCol + startCol;
			for(int j=endCol;true;j++) {
				if (row.getCell(j) != null && row.getCell(startCol + j - endCol) != null) {
					copyCell(workbook, row.getCell(j), row.getCell(startCol + j - endCol), true);
					sheet.setColumnWidth(startCol + j - endCol, sheet.getColumnWidth(j));
					if(j > newCellNum){
						if (row.getCell(j) != null) {
							row.removeCell(row.getCell(j));
						}
					}
				} else if (row.getCell(j) == null && row.getCell(startCol + j - endCol) != null) {
					row.removeCell(row.getCell(startCol + j - endCol));
					if(j > newCellNum){
						if (row.getCell(j) != null) {
							row.removeCell(row.getCell(j));
						}
					}
				} else if (row.getCell(j) != null && row.getCell(startCol + j - endCol) == null) {
					copyCell(workbook, row.getCell(j), row.createCell(startCol + j - endCol), true);
					sheet.setColumnWidth(startCol + j - endCol, sheet.getColumnWidth(j));
					if(j > newCellNum){
						if (row.getCell(j) != null) {
							row.removeCell(row.getCell(j));
						}
					}
				}
				if (startCol + j - endCol >= cellNum) {
					break;
				}
				if (startCol + j - endCol >= endCol && j >= cellNum) {
					// 目標のcellは削除対象ではなく、且つ操作のcellは元のデータ以外の場合
					break;
				}
			}
		}
	}*/
	
	/**
	 * 行を非表示になる   インデックスは 「0」から 「endRow」の行を含めない
	 * @param sheetName シート名
	 * @param startRow 非表示する行の開始インデックス from 0
	 * @param endRow 非表示する行の終了インデックス from 0
	 * @return
	 */
	public void hideRow(String sheetName,int startRow,int endRow) throws efwException{
		Sheet sheet = this.workbook.getSheet(sheetName);
		for(int i=startRow;i<=endRow;i++) {
			sheet.getRow(i).setZeroHeight(true);
		}
	}
	
	/**
	 * 行を表示になる   行数は 「0」から 「endRow」の行を含めない
	 * @param sheetName シート名
	 * @param startRow 表示する行の開始インデックス from 0
	 * @param endRow 表示する行の終了インデックス from 0
	 * @return
	 */
	public void showRow(String sheetName,int startRow,int endRow) throws efwException{
		Sheet sheet = this.workbook.getSheet(sheetName);
		for(int i=startRow;i<=endRow;i++) {
			sheet.getRow(i).setZeroHeight(false);
		}
	}
	/**
	 * 列の非表示
	 * @param sheetName シート名
	 * @param startCol 非表示する列の開始インデックス from 0
	 * @param endCol 非表示する列の終了インデックス from 0
	 * @return
	 */
	public void hideCol(String sheetName,int startCol,int endCol){
		Sheet sheet = this.workbook.getSheet(sheetName);
		for(int i=startCol;i<=endCol;i++){
			sheet.setColumnHidden(i, true);
		}
	}
	/**
	 * 列の表示
	 * @param sheetName シート名
	 * @param startCol 表示する列の開始インデックス from 0
	 * @param endCol 表示する列の終了インデックス from 0
	 * @return
	 */
	public void showCol(String sheetName,int startCol,int endCol){
		Sheet sheet = this.workbook.getSheet(sheetName);
		for(int i=startCol;i<=endCol;i++){
			sheet.setColumnHidden(i, false);
		}
	}
	/**
	 * シートの非表示
	 * @param sheetName シート名
	 */
	public void hideSheet(String sheetName){
		this.workbook.setSheetHidden(this.workbook.getSheetIndex(sheetName), true);
	}
	/**
	 * シートの表示
	 * @param sheetName シート名
	 */
	public void showSheet(String sheetName){
		this.workbook.setSheetHidden(this.workbook.getSheetIndex(sheetName), false);
	}

	
	/**
	 * XSSFのShapeをコピーする
	 * @param patriarch
	 * @param templateShape
	 * @return　作成されたshape
	 */
	private static XSSFSimpleShape cloneShape(XSSFDrawing patriarch,XSSFSimpleShape templateShape){
		XSSFSimpleShape shape = patriarch.createSimpleShape((XSSFClientAnchor)templateShape.getAnchor());
		shape.getCTShape().set(templateShape.getCTShape().copy());
		if(templateShape.getTextParagraphs().size()>0){
			XSSFTextParagraph tempParagraph=templateShape.getTextParagraphs().get(0);
			if(tempParagraph.getTextRuns().size()>0){
				XSSFTextRun tempRun=tempParagraph.getTextRuns().get(0);
				shape.setText(tempRun.getText());
				if(shape.getTextParagraphs().size()>0){
					XSSFTextParagraph paragraph=shape.getTextParagraphs().get(0);
					if(paragraph.getTextRuns().size()>0){
						XSSFTextRun textRun= paragraph.getTextRuns().get(0);
						textRun.setText(tempRun.getText());
						textRun.setFontSize(tempRun.getFontSize());
						textRun.setCharacterSpacing(tempRun.getCharacterSpacing());
						textRun.setFontColor(tempRun.getFontColor());
						textRun.setFontFamily(tempRun.getFontFamily(), Font.DEFAULT_CHARSET, tempRun.getPitchAndFamily(), false);
						paragraph.setTextAlign(tempParagraph.getTextAlign());
						paragraph.setTextFontAlign(tempParagraph.getTextFontAlign());
					}
				}
			}
		}
		return shape;
	}
	/**
	 * XSSFのShape名を取得する。
	 * @param shape 
	 * @return shape名
	 */
	private static String getShapeName(XSSFSimpleShape shape){
		return shape.getCTShape().getNvSpPr().getCNvPr().getName();
	}
	
	/**
	 * ExcelをPDFに変更する
	 * @param path 変更されたパスファイル名。storageからの相対パス
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 * @throws EncryptedDocumentException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void toPdf(String path) throws IOException, EncryptedDocumentException, InvalidFormatException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		File tempFile=File.createTempFile("efw", ".xlsx");
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(tempFile);
			workbook.write(out);
			Class op = Class.forName("efw.excel.OpenOffice");
			Method method = op.getDeclaredMethod("toPdf",File.class,File.class);
			method.invoke(null,tempFile,FileManager.get(path));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
				tempFile.delete();
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}
	
}
