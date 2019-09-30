<!DOCTYPE html>
<html>
<head>
    <title>PayCore检测</title>
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
            $("#xtjccore").addClass("active")
        });


    </script>

</head>
<body>
<#--页头-->
<#include "../commons/header.ftl">
<div class="container-fluid">
    <div>



    <#if heartBeatList??>
        <div class="text-center hidden">
            <a role="button"  data-toggle="collapse" href="#collapsePageInfo" aria-expanded="false" aria-controls="collapseExample" >=-分页信息-= </a>
        </div>

        <div class="panel-group" id="accordion">
            <div class="panel panel-info">
                <div class="panel-heading">
                    <h4 class="panel-title"> 共${heartBeatList?size}条。 </h4>
                </div>

            </div>
        </div>
        <table class="table table-bordered table-hover table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
            <thead>
            <tr style="background-color: #337ab7;color:white;"  class="bg-info">
                <th  style="width: 1%;" class="text-center" >序</th>
                <th  style="width: 7%;" class="text-center" >serverID</th>
                <th  style="width: 3%;" class="text-center" >branch</th>
                <th  style="width: 5%;" class="text-center" >gitCommitId</th>
                <th  style="width: 4%;" class="text-center" >gitCommitDate</th>
                <th  style="width: 4%;" class="text-center" >startDate</th>
                <th  style="width: 4%;" class="text-center" >HBTime</th>
            </tr>
            </thead>
            <tbody  id="datasline">
                <#list heartBeatList as heartBeat>
                <tr>
                    <td>${heartBeat_index!}</td>
                    <td>${heartBeat['serverID']!''}</td>
                    <td>${heartBeat['branch']!''}</td>
                    <td>${heartBeat['gitCommitId']!''}</td>
                    <td>${heartBeat['gitCommitDate']!''}</td>
                    <td>${heartBeat['startDate']!''}</td>
                    <td>${heartBeat['HBTime']!''}</td>
                </#list>
            </tbody>
        </table>

    </#if>
    </div>
    <div class="push"></div>
</div>

</body>
</html>