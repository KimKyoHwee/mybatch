package com.kyohwee.batch.project.batch;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

//엑셀에서 Row 읽기

public class ExcelRowReader implements ItemStreamReader<Row> {

    private final String filePath;
    private FileInputStream fileInputStream;
    private Workbook workbook;
    private Iterator<Row> rowCursor;
    private int currentRowNumber;
    private final String CURRENT_ROW_KEY = "current.row.number";

    public ExcelRowReader(String filePath) throws IOException {

        this.filePath = filePath;
        this.currentRowNumber = 0;
    }

    //엑셀파일 열고 닫기 (1번씩 쓰임)
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {

        try {
            fileInputStream = new FileInputStream(filePath);  //선언한 파일 열기
            workbook = WorkbookFactory.create(fileInputStream);  //열어서 workbook에 저장
            Sheet sheet = workbook.getSheetAt(0);  //workbook에서 iterator 시작
            this.rowCursor = sheet.iterator();

            // 동일 배치 파라미터에 대해 특정 키 값 "current.row.number"의 값이 존재한다면 초기화
            if (executionContext.containsKey(CURRENT_ROW_KEY)) {
                currentRowNumber = executionContext.getInt(CURRENT_ROW_KEY);
            }

            // 위의 값을 가져와 이미 실행한 부분은 건너 뜀
            for (int i = 0; i < currentRowNumber && rowCursor.hasNext(); i++) {
                rowCursor.next();
            }

        } catch (IOException e) {
            throw new ItemStreamException(e);
        }
    }

 //   (데이터를 여는 구조)
    @Override
    public Row read() {

        if (rowCursor != null && rowCursor.hasNext()) {
            currentRowNumber++;
            return rowCursor.next();
        } else {
            return null;
        }
    }

    //몇번까지 읽었나 계속 최신화
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt(CURRENT_ROW_KEY, currentRowNumber);  //읽다가 중단되면 중단점 저장
    }

    @Override
    public void close() throws ItemStreamException {

        try {
            if (workbook != null) {
                workbook.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        } catch (IOException e) {
            throw new ItemStreamException(e);
        }
    }
}