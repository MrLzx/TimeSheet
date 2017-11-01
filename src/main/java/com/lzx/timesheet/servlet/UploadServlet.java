package com.lzx.timesheet.servlet;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class UploadServlet extends HttpServlet {
    private String fileName; //�洢�ϴ����ļ����� ���㵼����ʱ����
    private String[] mapKey = {"name", "date", "ontime", "offtime"}; //�洢excel�����ݵ�map keyֵ
    private String[] head = {"����", "����", "�ϰ�ʱ��", "�°�ʱ��", "�Ӱ����", "�Ӱ�����", "�Ӱ������ܼ�"}; //�����ļ���������
    private Map<Date, Date> mHoliday = new HashMap<Date, Date>(); //�洢�ڼ���
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            File upload = upload(req);

            if (upload != null) {
                List<Map<String, String>> list = excelToList(upload);
                Map<String, List<Map<String, String>>> map = listToMap(list);
                doExport(resp, map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ����
     *
     * @param resp
     * @param mapTime
     * @throws Exception
     */
    private void doExport(HttpServletResponse resp, Map<String, List<Map<String, String>>> mapTime) throws Exception {
        resp.reset();
        resp.setContentType("application/x-msdownload");
        resp.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet();

        HSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER); // ����
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);//��ֱ

        //���ɱ�ͷ
        HSSFRow row = sheet.createRow(0);

        for (int i = 0; i < head.length; i++) {
            HSSFCell cell = row.createCell(i);
            cell.setCellValue(head[i]);
        }

        int index = 1; //��¼excel����
        int beginRow = 1; //��¼ÿ���˵���ʼ��
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        Iterator<Map.Entry<String, List<Map<String, String>>>> iterator = mapTime.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, List<Map<String, String>>> entry = iterator.next();

            List<Map<String, String>> list = entry.getValue();
            double dOvertimeDayAll = 0; //�Ӱ������ܼ�

            for (Map<String, String> map : list) {
                row = sheet.createRow(index);

                String date = map.get(mapKey[1]); //����
                date = date.replace("/","-");
                Date dDate = dateFormat.parse(date); //����
                date = dateFormat.format(dDate);
                map.put(mapKey[1], date);

                String ontime = map.get(mapKey[2]); //�ϰ�ʱ��
                String offtime = map.get(mapKey[3]); //�°�ʱ��
                long nOvertimeMinutes = 0; //�Ӱ����
                double dOvertimeDay = 0; //�Ӱ�����

                for (int i = 0; i < head.length; i++) {
                    HSSFCell cell = row.createCell(i);

                    switch (i) {
                        case 4: //�Ӱ����
                            if (ontime != null && !ontime.equals("") && offtime != null && !offtime.equals("")) {
                                long lOntime = timeFormat.parse(ontime).getTime(); //�ϰ����
                                long lOfftime = timeFormat.parse(offtime).getTime(); //�°����

                                if (mHoliday.containsKey(dDate)) { //�ڼ���
                                    nOvertimeMinutes = (lOfftime - lOntime) / 1000 / 60;
                                } else { //������
                                    long l1 = (timeFormat.parse("09:30").getTime() - lOntime) / 1000 / 60;
                                    l1 = l1 < 0 ? 0 : l1;
                                    long l2 = (lOfftime - timeFormat.parse("19:00").getTime()) / 1000 / 60;
                                    l2 = l2 < 0 ? 0 : l2;
                                    nOvertimeMinutes = l1 + l2;
                                }
                            }

                            cell.setCellValue(nOvertimeMinutes);
                            break;
                        case 5: //�Ӱ�����
                            if (nOvertimeMinutes >= 60 && nOvertimeMinutes < 120) {
                                dOvertimeDay = 0.5;
                            } else if (nOvertimeMinutes >= 120) {
                                dOvertimeDay = 1;
                            }

                            cell.setCellValue(dOvertimeDay);
                            break;
                        case 6: //�Ӱ������ܼ�
                            dOvertimeDayAll += dOvertimeDay;
                            break;
                        default:
                            cell.setCellValue(map.get(mapKey[i]));
                            break;
                    }
                }

                index++;
            }

            if (list.size() > 1) {
                row = sheet.getRow(beginRow);
                HSSFCell cell = row.createCell(6);
                cell.setCellValue(dOvertimeDayAll);
                cell.setCellStyle(cellStyle);

                CellRangeAddress cellRangeAddress = new CellRangeAddress(beginRow, beginRow + list.size() - 1, 6, 6);
                sheet.addMergedRegion(cellRangeAddress);

            }

            beginRow += list.size();
        }

        //������Ӧ���
        for (int i = 0; i < head.length; i++) {
            sheet.autoSizeColumn(i);
        }

        OutputStream out = null;

        try {
            out = resp.getOutputStream();
            workbook.write(out);
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
    }

    /**
     * listתmap keyֵΪ���� ����ͳ����ϲ���
     *
     * @param list
     * @return
     * @throws Exception
     */
    private Map<String, List<Map<String, String>>> listToMap(List<Map<String, String>> list) throws Exception {
        Map<String, List<Map<String, String>>> map = new LinkedHashMap<String, List<Map<String, String>>>();

        for (Map<String, String> stringMap : list) {
            String name = stringMap.get(mapKey[0]);
            List<Map<String, String>> mapList = new ArrayList<Map<String, String>>();

            if (map.containsKey(name)) {
                mapList = map.get(name);
            }

            mapList.add(stringMap);
            map.put(name, mapList);
        }

        return map;
    }

    /**
     * ����excel��ת��list
     *
     * @param file
     * @return
     * @throws Exception
     */
    private List<Map<String, String>> excelToList(File file) throws Exception {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        HSSFWorkbook workbook = new HSSFWorkbook(FileUtils.openInputStream(file));
        HSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        int colNum = 4; //��Ҫ������������
        int i = 0;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            if (i == 0) {
                i++;
                continue;
            }

            Map<String, String> map = new HashMap<String, String>();

            for (int j = 0; j < colNum; j++) {
                Cell cell = row.getCell(j);

                if (j == 0 && (cell == null || cell.getStringCellValue().trim().equals(""))) {
                    break;
                }

                String value = null;

                if (cell != null) {
                    value = cell.getStringCellValue().trim();
                }

                map.put(mapKey[j], value);
            }

            if (!map.isEmpty()) {
                list.add(map);
            }

            i++;
        }

        return list;
    }

    /**
     * �ϴ��ļ�,��ΪĿǰֻ�����ϴ������ļ�,���Է��ص�fileҲֻ��һ��
     *
     * @param req
     * @return
     * @throws Exception
     */
    private File upload(HttpServletRequest req) throws Exception {
        File file = null;
        //�õ��ϴ��ļ��ı���Ŀ¼�����ϴ����ļ������WEB-INFĿ¼�£����������ֱ�ӷ��ʣ���֤�ϴ��ļ��İ�ȫ
        String savePath = this.getServletContext().getRealPath("/WEB-INF/upload");
        //�ϴ�ʱ���ɵ���ʱ�ļ�����Ŀ¼
        String tempPath = this.getServletContext().getRealPath("/WEB-INF/temp");

        File tmpFile = new File(tempPath);

        if (!tmpFile.exists()) {
            //������ʱĿ¼
            tmpFile.mkdir();
        }

        //ʹ��Apache�ļ��ϴ���������ļ��ϴ����裺
        //1������һ��DiskFileItemFactory����
        DiskFileItemFactory factory = new DiskFileItemFactory();
        //���ù����Ļ������Ĵ�С�����ϴ����ļ���С�����������Ĵ�Сʱ���ͻ�����һ����ʱ�ļ���ŵ�ָ������ʱĿ¼���С�
        factory.setSizeThreshold(1024 * 100);//���û������Ĵ�СΪ100KB�������ָ������ô�������Ĵ�СĬ����10KB
        //�����ϴ�ʱ���ɵ���ʱ�ļ��ı���Ŀ¼
        factory.setRepository(tmpFile);
        //2������һ���ļ��ϴ�������
        ServletFileUpload upload = new ServletFileUpload(factory);
        //�����ļ��ϴ�����
        upload.setProgressListener(new ProgressListener() {
            public void update(long pBytesRead, long pContentLength, int arg2) {
                System.out.println("�ļ���СΪ��" + pContentLength + ",��ǰ�Ѵ���" + pBytesRead);
            }
        });
        //����ϴ��ļ�������������
        upload.setHeaderEncoding("UTF-8");

        //3���ж��ύ�����������Ƿ����ϴ���������
        /*if (!ServletFileUpload.isMultipartContent(req)) {
            //���մ�ͳ��ʽ��ȡ����
            return;
        }*/

        /*//�����ϴ������ļ��Ĵ�С�����ֵ��Ŀǰ������Ϊ1024*1024�ֽڣ�Ҳ����1MB
        upload.setFileSizeMax(1024*1024);
        //�����ϴ��ļ����������ֵ�����ֵ=ͬʱ�ϴ��Ķ���ļ��Ĵ�С�����ֵ�ĺͣ�Ŀǰ����Ϊ10MB
        upload.setSizeMax(1024*1024*10);*/

        //4��ʹ��ServletFileUpload�����������ϴ����ݣ�����������ص���һ��List<FileItem>���ϣ�ÿһ��FileItem��Ӧһ��Form����������
        List<FileItem> list = upload.parseRequest(req);

        for (FileItem item : list) {
            //���fileitem�з�װ������ͨ�����������
            if (item.isFormField()) {
                String name = item.getFieldName();
                //�����ͨ����������ݵ�������������
                String value = item.getString("UTF-8");
                mHoliday.put(dateFormat.parse(value), dateFormat.parse(value));
            } else {
                //�õ��ϴ����ļ�����
                String filename = item.getName();
                System.out.println(filename);

                if (filename == null || filename.trim().equals("")) {
                    continue;
                }

                //ע�⣺��ͬ��������ύ���ļ����ǲ�һ���ģ���Щ������ύ�������ļ����Ǵ���·���ģ��磺  c:\a\b\1.txt������Щֻ�ǵ������ļ������磺1.txt
                //�����ȡ�����ϴ��ļ����ļ�����·�����֣�ֻ�����ļ�������
                filename = filename.substring(filename.lastIndexOf("\\") + 1);
                fileName = filename;
                //�õ��ϴ��ļ�����չ��
                String fileExtName = filename.substring(filename.lastIndexOf(".") + 1);
                //�����Ҫ�����ϴ����ļ����ͣ���ô����ͨ���ļ�����չ�����ж��ϴ����ļ������Ƿ�Ϸ�
                System.out.println("�ϴ����ļ�����չ���ǣ�" + fileExtName);
                //��ȡitem�е��ϴ��ļ���������
                InputStream in = item.getInputStream();
                //�õ��ļ����������
                String saveFilename = makeFileName(filename);
                //�õ��ļ��ı���Ŀ¼
                String realSavePath = makePath(saveFilename, savePath);
                //����һ���ļ������
                FileOutputStream out = new FileOutputStream(realSavePath + "\\" + saveFilename);
                //����һ��������
                byte buffer[] = new byte[1024];
                //�ж��������е������Ƿ��Ѿ�����ı�ʶ
                int len;
                //ѭ�������������뵽���������У�(len=in.read(buffer))>0�ͱ�ʾin���滹������
                while ((len = in.read(buffer)) > 0) {
                    //ʹ��FileOutputStream�������������������д�뵽ָ����Ŀ¼(savePath + "\\" + filename)����
                    out.write(buffer, 0, len);
                }
                //�ر�������
                in.close();
                //�ر������
                out.close();
                //ɾ�������ļ��ϴ�ʱ���ɵ���ʱ�ļ�
                item.delete();
                file = new File(realSavePath + "\\" + saveFilename);
            }
        }

        return file;
    }

    /**
     * �����ϴ��ļ����ļ������ļ����ԣ�uuid+"_"+�ļ���ԭʼ����
     *
     * @param filename �ļ���ԭʼ����
     * @return uuid+"_"+�ļ���ԭʼ����
     */
    private String makeFileName(String filename) {
        return UUID.randomUUID().toString() + "_" + filename;
    }

    /**
     * Ϊ��ֹһ��Ŀ¼�������̫���ļ���Ҫʹ��hash�㷨��ɢ�洢
     *
     * @param filename �ļ�����Ҫ�����ļ������ɴ洢Ŀ¼
     * @param savePath �ļ��洢·��
     * @return �µĴ洢Ŀ¼
     */
    private String makePath(String filename, String savePath) {
        //�õ��ļ�����hashCode��ֵ���õ��ľ���filename����ַ����������ڴ��еĵ�ַ
        int hashcode = filename.hashCode();
        int dir1 = hashcode & 0xf;  //0--15
        int dir2 = (hashcode & 0xf0) >> 4;  //0-15
        //�����µı���Ŀ¼
        String dir = savePath + "\\" + dir1 + "\\" + dir2;  //upload\2\3  upload\3\5
        //File�ȿ��Դ����ļ�Ҳ���Դ���Ŀ¼
        File file = new File(dir);
        //���Ŀ¼������
        if (!file.exists()) {
            //����Ŀ¼
            file.mkdirs();
        }
        return dir;
    }
}