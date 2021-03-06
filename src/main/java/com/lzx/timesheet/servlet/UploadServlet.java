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
    private String fileName; //存储上传的文件名称 方便导出的时候用
    private String[] mapKey = {"name", "date", "ontime", "offtime"}; //存储excel行数据的map key值
    private String[] head = {"姓名", "日期", "上班时间", "下班时间", "加班分钟", "加班天数", "加班天数总计"}; //导出文件的列名称
    private Map<Date, Date> mHoliday = new HashMap<Date, Date>(); //存储节假日
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
     * 导出
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
        cellStyle.setAlignment(HorizontalAlignment.CENTER); // 居中
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);//垂直

        //生成表头
        HSSFRow row = sheet.createRow(0);

        for (int i = 0; i < head.length; i++) {
            HSSFCell cell = row.createCell(i);
            cell.setCellValue(head[i]);
        }

        int index = 1; //记录excel行数
        int beginRow = 1; //记录每个人的起始行
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        Iterator<Map.Entry<String, List<Map<String, String>>>> iterator = mapTime.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, List<Map<String, String>>> entry = iterator.next();

            List<Map<String, String>> list = entry.getValue();
            double dOvertimeDayAll = 0; //加班天数总计

            for (Map<String, String> map : list) {
                row = sheet.createRow(index);

                String date = map.get(mapKey[1]); //日期
                date = date.replace("/","-");
                Date dDate = dateFormat.parse(date); //日期
                date = dateFormat.format(dDate);
                map.put(mapKey[1], date);

                String ontime = map.get(mapKey[2]); //上班时间
                String offtime = map.get(mapKey[3]); //下班时间
                long nOvertimeMinutes = 0; //加班分钟
                double dOvertimeDay = 0; //加班天数

                for (int i = 0; i < head.length; i++) {
                    HSSFCell cell = row.createCell(i);

                    switch (i) {
                        case 4: //加班分钟
                            if (ontime != null && !ontime.equals("") && offtime != null && !offtime.equals("")) {
                                long lOntime = timeFormat.parse(ontime).getTime(); //上班毫秒
                                long lOfftime = timeFormat.parse(offtime).getTime(); //下班毫秒

                                if (mHoliday.containsKey(dDate)) { //节假日
                                    nOvertimeMinutes = (lOfftime - lOntime) / 1000 / 60;
                                } else { //工作日
                                    long l1 = (timeFormat.parse("09:30").getTime() - lOntime) / 1000 / 60;
                                    l1 = l1 < 0 ? 0 : l1;
                                    long l2 = (lOfftime - timeFormat.parse("19:00").getTime()) / 1000 / 60;
                                    l2 = l2 < 0 ? 0 : l2;
                                    nOvertimeMinutes = l1 + l2;
                                }
                            }

                            cell.setCellValue(nOvertimeMinutes);
                            break;
                        case 5: //加班天数
                            if (nOvertimeMinutes >= 60 && nOvertimeMinutes < 120) {
                                dOvertimeDay = 0.5;
                            } else if (nOvertimeMinutes >= 120) {
                                dOvertimeDay = 1;
                            }

                            cell.setCellValue(dOvertimeDay);
                            break;
                        case 6: //加班天数总计
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

        //列自适应宽度
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
     * list转map key值为姓名 方便统计与合并行
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
     * 解析excel并转成list
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
        int colNum = 4; //需要解析的总列数
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
     * 上传文件,因为目前只做了上传单个文件,所以返回的file也只有一个
     *
     * @param req
     * @return
     * @throws Exception
     */
    private File upload(HttpServletRequest req) throws Exception {
        File file = null;
        //得到上传文件的保存目录，将上传的文件存放于WEB-INF目录下，不允许外界直接访问，保证上传文件的安全
        String savePath = this.getServletContext().getRealPath("/WEB-INF/upload");
        //上传时生成的临时文件保存目录
        String tempPath = this.getServletContext().getRealPath("/WEB-INF/temp");

        File tmpFile = new File(tempPath);

        if (!tmpFile.exists()) {
            //创建临时目录
            tmpFile.mkdir();
        }

        //使用Apache文件上传组件处理文件上传步骤：
        //1、创建一个DiskFileItemFactory工厂
        DiskFileItemFactory factory = new DiskFileItemFactory();
        //设置工厂的缓冲区的大小，当上传的文件大小超过缓冲区的大小时，就会生成一个临时文件存放到指定的临时目录当中。
        factory.setSizeThreshold(1024 * 100);//设置缓冲区的大小为100KB，如果不指定，那么缓冲区的大小默认是10KB
        //设置上传时生成的临时文件的保存目录
        factory.setRepository(tmpFile);
        //2、创建一个文件上传解析器
        ServletFileUpload upload = new ServletFileUpload(factory);
        //监听文件上传进度
        upload.setProgressListener(new ProgressListener() {
            public void update(long pBytesRead, long pContentLength, int arg2) {
                System.out.println("文件大小为：" + pContentLength + ",当前已处理：" + pBytesRead);
            }
        });
        //解决上传文件名的中文乱码
        upload.setHeaderEncoding("UTF-8");

        //3、判断提交上来的数据是否是上传表单的数据
        /*if (!ServletFileUpload.isMultipartContent(req)) {
            //按照传统方式获取数据
            return;
        }*/

        /*//设置上传单个文件的大小的最大值，目前是设置为1024*1024字节，也就是1MB
        upload.setFileSizeMax(1024*1024);
        //设置上传文件总量的最大值，最大值=同时上传的多个文件的大小的最大值的和，目前设置为10MB
        upload.setSizeMax(1024*1024*10);*/

        //4、使用ServletFileUpload解析器解析上传数据，解析结果返回的是一个List<FileItem>集合，每一个FileItem对应一个Form表单的输入项
        List<FileItem> list = upload.parseRequest(req);

        for (FileItem item : list) {
            //如果fileitem中封装的是普通输入项的数据
            if (item.isFormField()) {
                String name = item.getFieldName();
                //解决普通输入项的数据的中文乱码问题
                String value = item.getString("UTF-8");
                mHoliday.put(dateFormat.parse(value), dateFormat.parse(value));
            } else {
                //得到上传的文件名称
                String filename = item.getName();
                System.out.println(filename);

                if (filename == null || filename.trim().equals("")) {
                    continue;
                }

                //注意：不同的浏览器提交的文件名是不一样的，有些浏览器提交上来的文件名是带有路径的，如：  c:\a\b\1.txt，而有些只是单纯的文件名，如：1.txt
                //处理获取到的上传文件的文件名的路径部分，只保留文件名部分
                filename = filename.substring(filename.lastIndexOf("\\") + 1);
                fileName = filename;
                //得到上传文件的扩展名
                String fileExtName = filename.substring(filename.lastIndexOf(".") + 1);
                //如果需要限制上传的文件类型，那么可以通过文件的扩展名来判断上传的文件类型是否合法
                System.out.println("上传的文件的扩展名是：" + fileExtName);
                //获取item中的上传文件的输入流
                InputStream in = item.getInputStream();
                //得到文件保存的名称
                String saveFilename = makeFileName(filename);
                //得到文件的保存目录
                String realSavePath = makePath(saveFilename, savePath);
                //创建一个文件输出流
                FileOutputStream out = new FileOutputStream(realSavePath + "\\" + saveFilename);
                //创建一个缓冲区
                byte buffer[] = new byte[1024];
                //判断输入流中的数据是否已经读完的标识
                int len;
                //循环将输入流读入到缓冲区当中，(len=in.read(buffer))>0就表示in里面还有数据
                while ((len = in.read(buffer)) > 0) {
                    //使用FileOutputStream输出流将缓冲区的数据写入到指定的目录(savePath + "\\" + filename)当中
                    out.write(buffer, 0, len);
                }
                //关闭输入流
                in.close();
                //关闭输出流
                out.close();
                //删除处理文件上传时生成的临时文件
                item.delete();
                file = new File(realSavePath + "\\" + saveFilename);
            }
        }

        return file;
    }

    /**
     * 生成上传文件的文件名，文件名以：uuid+"_"+文件的原始名称
     *
     * @param filename 文件的原始名称
     * @return uuid+"_"+文件的原始名称
     */
    private String makeFileName(String filename) {
        return UUID.randomUUID().toString() + "_" + filename;
    }

    /**
     * 为防止一个目录下面出现太多文件，要使用hash算法打散存储
     *
     * @param filename 文件名，要根据文件名生成存储目录
     * @param savePath 文件存储路径
     * @return 新的存储目录
     */
    private String makePath(String filename, String savePath) {
        //得到文件名的hashCode的值，得到的就是filename这个字符串对象在内存中的地址
        int hashcode = filename.hashCode();
        int dir1 = hashcode & 0xf;  //0--15
        int dir2 = (hashcode & 0xf0) >> 4;  //0-15
        //构造新的保存目录
        String dir = savePath + "\\" + dir1 + "\\" + dir2;  //upload\2\3  upload\3\5
        //File既可以代表文件也可以代表目录
        File file = new File(dir);
        //如果目录不存在
        if (!file.exists()) {
            //创建目录
            file.mkdirs();
        }
        return dir;
    }
}