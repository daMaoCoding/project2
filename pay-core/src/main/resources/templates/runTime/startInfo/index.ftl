<!DOCTYPE html>
<html>
<head>
    <title>请求支付流水表</title>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <link rel="shortcut icon" href="${request.contextPath}/static/favicon.ico">
    <!--bootStrap -->
    <link href="${request.contextPath}/static/bootstrap-3.3.7/css/bootstrap.min.css" rel="stylesheet">
    <!-- HTML5 Shim 和 Respond.js 用于让 IE8 支持 HTML5元素和媒体查询 -->
    <!-- 注意： 如果通过 file://  引入 Respond.js 文件，则该文件无法起效果 -->
    <!--[if lt IE 9]>
    <script src="${request.contextPath}/static/html5shiv/3.7.0/html5shiv.js"></script>
    <script src="${request.contextPath}/static/respond.js/1.3.0/respond.min.js"></script>
    <![endif]-->
    <script src="${request.contextPath}/static/js/jquery-1.11.1.min.js"></script>
    <script src="${request.contextPath}/static/bootstrap-3.3.7/js/bootstrap.min.js"></script>

<#--<link href="${request.contextPath}/static/css/style_pay.css" rel="stylesheet" type="text/css"/>-->

    <style type="text/css">
        td {white-space:nowrap;overflow:hidden;word-break:keep-all;text-overflow:ellipsis}
        .table-hover>tbody>tr:hover { background-color: wheat!important;}

    </style>
    <script>
        $(function () {
            $("#navLi li").removeClass("active")
            $("#qdzt").addClass("active")
        });


    </script>

</head>
<body>
<#--页头-->
<#include "../../commons/header.ftl">
<div class="container-fluid">
    <div>

        <form  class="form-inline"  action="${request.contextPath}/runTime/" method="post">
            <table class="table-bordered table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
                <tr>
                    <th class="text-right">gitCommitId：</th>
                    <td>
                        <input  style="width: 100%;" class="form-control input-sm"  type="text" name="gitCommitId" id="gitCommitId"  value="<#if queryParam.gitCommitId??>${queryParam.gitCommitId}</#if>"/>
                    </td>
                    <th class="text-right">profiles：</th>
                    <td>
                        <input  style="width: 100%;" class="form-control input-sm"  type="text" name="profiles" value="<#if queryParam.profiles??>${queryParam.profiles}</#if>"/>
                    </td>
                    <td rowspan="2" style="width: 9%;">
                        <button type="submit" class="btn btn-success center-block" ><span class="glyphicon glyphicon-send"></span> 查询</button>
                    </td>
                </tr>

                <tr>
                    <th class="text-right">页码：</th>
                    <td><input   style="width: 100%;" class="form-control input-sm" type="text" name="page" value="${(page!0)?c}"/></td>
                    <th class="text-right">页面大小：</th>
                    <td><input   style="width: 100%;" class="form-control input-sm" type="text" name="rows" value="${(rows!10)?c}"/></td>
                </tr>
            </table>
        </form>

    <#if pageInfo??>
        <div class="text-center hidden">
            <a role="button"  data-toggle="collapse" href="#collapsePageInfo" aria-expanded="false" aria-controls="collapseExample" >=-分页信息-= </a>
        </div>

        <div class="panel-group" id="accordion">
            <div class="panel panel-info">
                <div class="panel-heading">
                    <h4 class="panel-title"> 共${pageInfo.total}条。 </h4>
                </div>

            </div>
        </div>


        <table class="table table-bordered table-hover table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
            <thead>
            <tr style="background-color: #337ab7;color:white;"  class="bg-info">
                <th  style="width: 1%;" class="text-center" >序</th>
                <th  style="width: 2%;" class="text-center" >ID</th>
                <th  style="width: 5%;" class="text-center" >appname</th>
                <th  style="width: 5%;" class="text-center" >profiles</th>
                <th  style="width: 5%;" class="text-center" >serverID</th>
                <th  style="width: 5%;" class="text-center" >startDateTime</th>
                <th  style="width: 5%;" class="text-center" >gitBranch</th>
                <th  style="width: 8%;" class="text-center" >gitCommitId</th>
                <th  style="width: 4%;" class="text-center" >gitCommitTime</th>
                <th  style="width: 5%;" class="text-center">操作</th>
            </tr>
            </thead>
            <tbody  id="datasline">
                <#list pageInfo.list as startInfo>
                <tr>
                    <td>${startInfo_index!}</td>
                    <td>${startInfo.id!?c}</td>
                    <td style="text-overflow: ellipsis">${startInfo.appname!}</td>
                    <td>${startInfo.profiles!}</td>
                    <td>${startInfo.serverID!}</td>
                    <td>${startInfo.startDateTime?string('yyyy-MM-dd HH:mm:ss')}</td>
                    <td>${startInfo.gitBranch!}</td>
                    <td>${startInfo.gitCommitId!}</td>
                    <td>${startInfo.gitCommitTime!}</td>
                    <td style="text-align:center;">[<a href="${request.contextPath}/runTime/view/${startInfo.id?c}">详情</a>]
                    </td>
                </tr>
                </#list>
            </tbody>
        </table>



    <nav class="text-right" aria-label="Page navigation">
        <ul class="pagination">
                <#if pageInfo.hasPreviousPage>
                    <li>
                        <a href="${request.contextPath}/runTime/?page=1&rows=${pageInfo.pageSize!?c}&gitCommitId=${queryParam.gitCommitId}&profiles=${queryParam.profiles}">首页</a>
                    </li>
                    <li>
                        <a href="${request.contextPath}/runTime/?page=${pageInfo.prePage!?c}&rows=${pageInfo.pageSize!?c}&gitCommitId=${queryParam.gitCommitId}&profiles=${queryParam.profiles}">前一页</a>
                    </li>
                </#if>
                <#list pageInfo.navigatepageNums as nav>
                    <#if nav == pageInfo.pageNum>
                        <li>
                            <a style="font-weight: bold;" href="#">${nav!?c}</a>
                    <li>
                    </#if>
                    <#if nav != pageInfo.pageNum>
                        <li>
                            <a href="${request.contextPath}/runTime/?page=${nav!?c}&rows=${pageInfo.pageSize!?c}&gitCommitId=<#if queryParam.gitCommitId??>${queryParam.gitCommitId}</#if>&profiles=<#if queryParam.profiles??>${queryParam.profiles}</#if>">${nav!?c}</a>
                        </li>
                    </#if>
                </#list>
                <#if pageInfo.hasNextPage>
                    <li>
                        <a href="${request.contextPath}/runTime/?page=${pageInfo.nextPage!?c}&rows=${pageInfo.pageSize!?c}&gitCommitId=<#if queryParam.gitCommitId??>${queryParam.gitCommitId}</#if>&profiles=<#if queryParam.profiles??>${queryParam.profiles}</#if>">下一页</a>
                    </li>
                    <li>
                        <a href="${request.contextPath}/runTime/?page=${pageInfo.pages!?c}&rows=${pageInfo.pageSize!?c}&gitCommitId=<#if queryParam.gitCommitId??>${queryParam.gitCommitId}</#if>&profiles=<#if queryParam.profiles??>${queryParam.profiles}</#if>">尾页</a>
                    </li>
                </#if>
            </li>
        </ul>
    </nav>



    </#if>
    </div>
    <div class="push"></div>
</div>
<script>
    $(function () {
        var firstLineGitCommitId = $("#datasline tr:eq(0)").children('td:eq(7)').text();
        $("#gitCommitId").val(firstLineGitCommitId);
    });
</script>
</body>
</html>