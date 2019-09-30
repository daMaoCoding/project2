<!DOCTYPE html>
<html>
<head>
    <title>PayApiUrl检测</title>
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
        td {white-space:nowrap;overflow:hidden;word-break:keep-all;text-overflow:ellipsis;}
        .table-hover>tbody>tr:hover { background-color: wheat!important;}

    </style>
    <script>
        $(function () {
            $("#navLi li").removeClass("active")
            $("#xtjcapiurl").addClass("active")
        });


    </script>

</head>
<body>
<#--页头-->
<#include "../commons/header.ftl">
<div class="container-fluid">
    <div>



    <#if payApiUrlList??>
        <div class="text-center hidden">
            <a role="button"  data-toggle="collapse" href="#collapsePageInfo" aria-expanded="false" aria-controls="collapseExample" >=-分页信息-= </a>
        </div>

        <div class="panel-group" id="accordion">
            <div class="panel panel-info">
                <div class="panel-heading">
                    <h4 class="panel-title"> 共${payApiUrlList?size}条。 </h4>
                </div>

            </div>
        </div>
        <table class="table table-bordered table-hover table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
            <thead>
            <tr style="background-color: #337ab7;color:white;"  class="bg-info">
                <th  style="width: 5%;" class="text-center" >序</th>
                <th  style="width: 5%;" class="text-center" >OID</th>
                <th  style="width: 30%;" class="text-center" >web_url</th>
                <th  style="width: 30%;" class="text-center" >jump_url</th>
                <th  style="width: 30%;" class="text-center" >notify_url</th>
            </tr>
            </thead>
            <tbody  id="datasline">


                <#list payApiUrlList as payApiUrl>
                <tr>
                    <td  style="vertical-align: middle;">${payApiUrl_index!}</td>
                    <td  style="vertical-align: middle;">${payApiUrl['OID']!''}</td>

                    <td>
                       <#if payApiUrl['web_url'] ??>
                            <#list payApiUrl['web_url'] as web_url>
                                <table>
                                    <tr><td> ${web_url}</td></tr>
                                </table>
                            </#list>
                       </#if>
                    </td>


                    <td>
                        <#if payApiUrl['jump_url'] ??>
                            <#list  payApiUrl['jump_url']?keys as key>
                                <table>
                                    <tr>
                                        <td <#if  payApiUrl['jump_url'][key]>  style="background-color: #dff0d8;" <#else>style="background-color: #f2dede;" </#if> > ${key}</td>
                                    </tr>
                                </table>
                            </#list>
                        </#if>
                    </td>


                    <td>
                        <#if payApiUrl['notify_url'] ??>
                            <#list  payApiUrl['notify_url']?keys as key>
                                <table>
                                    <tr>
                                        <td <#if  payApiUrl['notify_url'][key]>  style="background-color: #dff0d8;" <#else>style="background-color: #f2dede;" </#if> > ${key}</td>
                                    </tr>
                                </table>
                            </#list>
                        </#if>
                    </td>




                </#list>





            </tbody>
        </table>

    </#if>
    </div>
    <div class="push"></div>
</div>

</body>
</html>




