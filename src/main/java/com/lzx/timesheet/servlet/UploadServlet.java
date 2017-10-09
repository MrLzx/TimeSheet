package com.lzx.timesheet.servlet;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

public class UploadServlet extends HttpServlet {
    private String fileName; //�洢�ϴ����ļ����� ���㵼����ʱ����
    private String[] mapKey = {"name", "date", "ontime", "offtime"}; //�洢excel�����ݵ�map keyֵ
    private String[] head = {"����", "����", "�ϰ�ʱ��", "�°�ʱ��", "�Ӱ����", "�Ӱ�����"}; //�洢excel�����ݵ�map keyֵ

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            File upload = upload(req);
            List list = excelToList(upload);
            doExport(resp, list);
        } catch (Exception e) {
            e.printStackTrace();
        }

        req.getRequestDispatcher("/index.jsp").forward(req, resp);
    }

    private void doExport(HttpServletResponse resp, List<Map<String, String>> list) throws Exception {
        resp.reset();
        resp.setContentType("application/x-msdownload");
        resp.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet();

        //���ɱ�ͷ
        HSSFRow row = sheet.createRow(0);

        for (int i = 0; i < head.length; i++) {
            HSSFCell cell = row.createCell(i);
            cell.setCellValue(head[i]);
        }

        int index = 1;

        for (Map<String, String> map : list) {
            row = sheet.createRow(index);

            for (int i = 0; i < head.length; i++) {
                HSSFCell cell = row.createCell(i);

                switch (i) {
                    case 4:
                        cell.setCellValue("4");
                        break;
                    case 5:
                        cell.setCellValue("5");
                        break;
                    default:
                        cell.setCellValue(map.get(mapKey[i]));
                        break;
                }
            }

            index++;
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
     * ����excel��ת��list
     *
     * @param file
     * @return
     * @throws Exception
     */
    private List excelToList(File file) throws Exception {
        List<Map> list = new ArrayList<Map>();

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

        return file;
    }

    /**
     * �����ϴ��ļ����ļ������ļ����ԣ�uuid+"_"+�ļ���ԭʼ����
     *
     * @param filename �ļ���ԭʼ����
     * @return uuid+"_"+�ļ���ԭʼ����
     */
    private String makeFileName(String filename) {  //2.jpg
        //Ϊ��ֹ�ļ����ǵ���������ҪΪ�ϴ��ļ�����һ��Ψһ���ļ���
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