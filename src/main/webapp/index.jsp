<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<%
    String sYear = request.getParameter("year");
    String sMonth = request.getParameter("month");

    Calendar instance = Calendar.getInstance();
    instance.setTime(new Date());

    if (sYear == null || sYear.equals("")) {
        sYear = String.valueOf(instance.get(Calendar.YEAR));
    }

    if (sMonth == null || sMonth.equals("")) {
        sMonth = String.valueOf(instance.get(Calendar.MONTH) + 1);
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>考勤计算</title>
    <!-- Bootstrap -->
    <link href="css/bootstrap.min.css" rel="stylesheet">
    <link href="css/cover.css" rel="stylesheet">
    <!--[if lt IE 9]>
    <script src="https://cdn.bootcss.com/html5shiv/3.7.3/html5shiv.min.js"></script>
    <script src="https://cdn.bootcss.com/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->

    <script type="text/javascript">
        function toMonth(type) {
            var year = Number($("#year").val());
            var month = Number($("#month").val());

            if (type) {
                if (month > 1) {
                    month -= 1;
                } else {
                    year -= 1;
                    month = 12;
                }
            } else {
                if (month < 12) {
                    month += 1;
                } else {
                    year += 1;
                    month = 1;
                }
            }

            location.href = "index.jsp?year=" + year + "&month=" + month;
        }
    </script>
</head>
<body>
<div class="site-wrapper">
    <div class="site-wrapper-inner">
        <div class="cover-container">
            <div class="masthead clearfix">
                <div class="inner">
                    <h3>专门为黎大人做的考勤计算小工具</h3>
                </div>
            </div>

            <div class="inner cover">
                <button type="button" class="btn btn-default btn-xs" onclick="toMonth(true)">
                    <span class="glyphicon glyphicon-menu-left" aria-hidden="true"></span>
                </button>
                <strong style="margin: 0 20px;"><%=sYear%>年<%=sMonth%>月</strong>
                <input type="hidden" id="year" name="year" value="<%=sYear%>">
                <input type="hidden" id="month" name="month" value="<%=sMonth%>">
                <button type="button" class="btn btn-default btn-xs" onclick="toMonth(false)">
                    <span class="glyphicon glyphicon-menu-right" aria-hidden="true"></span>
                </button>

                <form method="post" enctype="multipart/form-data" action="upload.do" style="margin-top: 30px;">
                    <table class="table table-bordered">
                        <tr>
                            <td>星期一</td>
                            <td>星期二</td>
                            <td>星期三</td>
                            <td>星期四</td>
                            <td>星期五</td>
                            <td>星期六</td>
                            <td>星期日</td>
                        </tr>
                        <tr>
                            <%
                                int nYear = Integer.valueOf(sYear);
                                int nMonth = Integer.valueOf(sMonth);

                                instance.set(nYear, nMonth, 0);
                                int nLastDay = instance.get(Calendar.DATE); //当月最后一天

                                instance.set(nYear, nMonth - 1, 1);
                                int nWeek = instance.get(Calendar.DAY_OF_WEEK);
                                int nWeekDiff; //一号的星期差

                                if (nWeek == 1) {
                                    nWeekDiff = 6;
                                } else {
                                    nWeekDiff = nWeek - 2;
                                }

                                System.out.println("星期差:" + nWeekDiff);

                                for (int i = 0; i < nWeekDiff; i++) {
                                    out.print("<td></td>");
                                }

                                for (int i = 1; i <= nLastDay; i++) {
                                    String sDate = sYear + "-" + sMonth + "-" + i;

                                    out.print("<td>" +
                                            "<label class=\"checkbox-inline\">" +
                                            "<input type=\"checkbox\" id=\"dates\" name=\"dates\" value=\"" + sDate + "\"");

                                    //周末自动勾选
                                    if ((i + nWeekDiff) % 7 == 6 || (i + nWeekDiff) % 7 == 0) {
                                        out.print("checked=\"checked\"");
                                    }

                                    out.print(">" + i + "</label></td>");

                                    if ((i + nWeekDiff) % 7 == 0) {
                                        out.print("</tr>");
                                    }
                                }

                                int nLastDiff = 7 - (nLastDay + nWeekDiff) % 7; //末尾星期差

                                if (nLastDiff != 7) {
                                    for (int i = 0; i < 7 - (nLastDay + nWeekDiff) % 7; i++) {
                                        out.print("<td></td>");
                                    }
                                }
                            %>
                        </tr>
                    </table>

                    <div class="form-group text-left" style="margin-top: 50px;">
                        <label for="excel">选择考勤文件</label>
                        <input type="file" id="excel" name="excel" accept=".xls,.xlsx">
                    </div>

                    <input class="btn btn-default" type="submit" value="计算并导出" style="margin-top: 30px;">
                </form>
            </div>

            <div class="mastfoot">
                <div class="inner">
                    <p>Specially made for Li Lianghui</p>
                </div>
            </div>
        </div>
    </div>
</div>

<script type="text/javascript" language="JavaScript" src="js/jquery-3.2.1.min.js"></script>
<script src="js/bootstrap.min.js"></script>
</body>
</html>