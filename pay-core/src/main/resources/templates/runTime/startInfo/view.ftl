<html>
<head>
    <title>启动状态</title>
    <link rel="shortcut icon" href="${request.contextPath}/static/favicon.ico">
    <link href="${request.contextPath}/static/css/style_pay.css" rel="stylesheet" type="text/css"/>
    <script src="${request.contextPath}/static/js/jquery-1.11.1.min.js"></script>
    <style type="text/css">
        th{width: 212px;text-align: left;}
        textarea{width: 100%; height: 65px;}
    </style>
    <script>
        $(function () {
            $("#navLi li").removeClass("active")
            $("#qdzt").addClass("active")
        });
    </script>

</head>
<body style="margin-top:50px;overflow: hidden;">
<#--页头-->
<#include "../../commons/header.ftl">

        <table class="gridtable" style="width: 90%; margin-top: -50px;"  >
            <tr>
                <th style="text-align: center;">启动状态 - [<a href="${request.contextPath}/runTime/">返回</a>]</th>
            </tr>
            <#if msg??>
                <tr style="text-align: center;color:#00ba00;">
                    <th>${msg}</th>
                </tr>
            </#if>
        </table>


    <table class="gridtable" style="table-layout:fixed;width:90%;">

        <tr>
            <th style="background: lightsteelblue;">serverID</th> <td> <input type="text"   value="${startInfo.serverID!}"/></td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;">appname</th> <td> <input type="text"   value="${startInfo.appname!}"/></td>
        </tr>

        <tr>
            <th style="background: lightsteelblue;">profiles</th> <td> <input type="text"   value="${startInfo.profiles!}"/></td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;">startDateTime</th><td><input type="text"  value="${startInfo.startDateTime?string('yyyy-MM-dd HH:mm:ss')}"/></td>
        </tr>

        <tr>
            <th style="background: lightsteelblue;">ipAddress</th> <td> <input type="text"   value="${startInfo.ipAddress!}"/></td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;">port</th> <td> <input type="text"   value="${startInfo.port!}"/></td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;">gitBranch</th> <td> <input type="text"   value="${startInfo.gitBranch!}"/></td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;">gitCommitId</th> <td> <input type="text"   value="${startInfo.gitCommitId!}"/></td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;">gitShortCommitId</th> <td> <input type="text"   value="${startInfo.gitShortCommitId!}"/></td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;">gitCommitTime</th> <td> <input type="text"   value="${startInfo.gitCommitTime!}"/></td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;">memoryInfo</th> <td> <input type="text"   value="${startInfo.memoryInfo!?html?js_string}"/></td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;width: 280px;">commondLine</th><td><textarea><#if startInfo.commondLine??>${startInfo.commondLine}</#if></textarea></td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;width: 280px;">dbInfo</th><td><textarea><#if startInfo.dbInfo??>${startInfo.dbInfo}</#if></textarea></td>
        </tr>

    </table>

</body>
</html>
