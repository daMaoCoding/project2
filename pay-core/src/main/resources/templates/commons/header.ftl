<!DOCTYPE html>
<html>
<head>
    <title>第三方支付监控</title>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <meta name="renderer" content="webkit">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="apple-mobile-web-app-status-bar-style" content="black">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="format-detection" content="telephone=no">
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

    <!--日期选择框 -->
    <script src="${request.contextPath}/static/layDate-v5.0.7/laydate.js"></script>

     <!--Echarts -->
    <script src="${request.contextPath}/static/js/echarts.common.min.js"></script>
</head>
<body>
<#--全局权限-->
<#include  "../commons/shiroPermissionConfig.ftl">

<div class="container-fluid" style="padding-bottom: 80px;">
    <nav class="navbar navbar-default navbar-fixed-top" role="navigation">
        <div class="container-fluid">
            <div class="navbar-header">
                <div class="navbar-brand" href="#">支付监控</div>
            </div>
            <div>
                <ul id="navLi" class="nav  nav-pills" ><#-- class="active"-->
                    <li style="margin-top: 3px;"  role="presentation"  id="qqzf"  ><a href="/reqPayList">请求支付</a></li>
                    <li style="margin-top: 3px;"  role="presentation"  id="xyzf"  ><a href="/resPayList">响应支付</a></li>
                    <#if ShowReqDaifuList??>
                        <li style="margin-top: 3px;"  role="presentation"  id="qqdf"  ><a href="/reqDaifuList">请求代付</a></li>
                    </#if>
                    <#if ShowResDaifuList??>
                        <li style="margin-top: 3px;"  role="presentation"  id="xydf"  ><a href="/resDaifuList">响应代付</a></li>
                    </#if>
                   <#if ShowTongJi??>
                        <li style="margin-top: 3px;"  role="presentation"  id="zftdtj"><a href="/tongji/index/">支付通道统计</a></li>
                   </#if>
                    <li style="margin-top: 3px;"  role="presentation"  id="qdzt"  ><a href="/runTime/">PayCore启动</a></li>
                    <li style="margin-top: 3px;"  role="presentation"  id="xtjccore"  ><a href="/runTime/paycoreheartbeat">PayCore检测</a></li>
                    <li style="margin-top: 3px;"  role="presentation"  id="xtjcrest"  ><a href="/runTime/payrestheartbeat">PayRest检测</a></li>
                    <#if ShowApiUrlCheck??>
                        <li style="margin-top: 3px;"  role="presentation"  id="xtjcapiurl"  ><a href="/runTime/payapiurlhealthcheck">PayApiUrl检测</a></li>
                    </#if>
                    <li style="margin-top: 3px;"  role="presentation"  id="tdts"  ><a href="/bills">通道调试</a></li>
                    <li style="margin-top: 3px;"  role="presentation"  id="zfcgl"  ><a href="/tongji/sortChannel/5">支付成功率排行</a></li>
                    <li style="margin-top: 3px;"  role="presentation"  id="tdpz"  ><a href="/channelConfig">通道配置JSON</a></li>
                    <@shiro.user>
                        <li class="pull-right" style="margin-top: 3px;"  role="presentation"  ><a href="/logout">退出</a></li>
                    </@shiro.user>
                    <@shiro.hasRole name="administrator">
                        <li class="pull-right" style="margin-top: 3px;"  role="presentation"  ><a href="/admin">管理</a></li>
                    </@shiro.hasRole>
                </ul>
            </div>

            <div>
            </div>

        </div>
    </nav>
</div>
</body>
</html>