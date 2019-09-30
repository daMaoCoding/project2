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
            $("#tdts").addClass("active")
        });


    </script>

</head>
<body>
<#--页头-->
<#include "../commons/header.ftl">
<div class="container-fluid">
    <div>
        <h1 class="text-center hidden"><a>通道测试.[支付订单列表]</a>:&:<a href="/channelConfig">通道配置JSON</a></h1>



        <form  class="form-inline"  action="${request.contextPath}/bills" method="post">
            <table class="table-bordered table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
                <tr>
                    <th class="text-right">订单号：</th>
                    <td>
                        <input  style="width: 100%;" class="form-control input-sm"  type="text" name="API_ORDER_ID"  value="<#if queryParam.API_ORDER_ID??>${queryParam.API_ORDER_ID}</#if>"/>
                    </td>
                    <th class="text-right">支付通道</th>
                    <td>
                        <input  style="width: 100%;" class="form-control input-sm"  type="text" name="API_CHANNEL_BANK_NAME" value="<#if queryParam.API_CHANNEL_BANK_NAME??>${queryParam.API_CHANNEL_BANK_NAME}</#if>"/>
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




        <!-- 折叠
        <div   class="menu_collapse_nav">
            <button data-v-34fe458a="" type="button" class="el-button el-button--text">
                <span><span ><i   class="fa fa-chevron-right"></i></span></span>
            </button>
        </div>
 -->

    <#if pageInfo??>
        <div class="text-center hidden">
            <a role="button"  data-toggle="collapse" href="#collapsePageInfo" aria-expanded="false" aria-controls="collapseExample" >=-分页信息-= </a>
        </div>

        <div class="panel-group" id="accordion">
            <div class="panel panel-info">
                <div class="panel-heading">
                    <h4 class="panel-title">
                        共${pageInfo.total}条。
                        <a style="position: initial;float:right;" href="${request.contextPath}/bills/add">[新增支付订单]</a>
                    </h4>

                </div>

            </div>
        </div>

        <div class="collapse" id="collapsePageInfo">
            <table class="table table-bordered table-hover table-striped table-condensed text-center" >
                <tr>
                    <th class="text-right"  style="width: 970px;">当前页号</th>
                    <td class="text-left">${pageInfo.pageNum}</td>
                </tr>
                <tr>
                    <th  class="text-right" >页面大小</th>
                    <td class="text-left">${pageInfo.pageSize?c}</td>
                </tr>
                <tr>
                    <th  class="text-right" >起始行号(>=)</th>
                    <td class="text-left">${pageInfo.startRow}</td>
                </tr>
                <tr>
                    <th  class="text-right" >终止行号(<=)</th>
                    <td class="text-left">${pageInfo.endRow}</td>
                </tr>
                <tr>
                    <th  class="text-right" >总结果数</th>
                    <td class="text-left">${pageInfo.total}</td>
                </tr>
                <tr>
                    <th  class="text-right" >总页数</th>
                    <td  class="text-left">${pageInfo.pages}</td>
                </tr>
                <tr>
                    <th  class="text-right" >第一页</th>
                    <td  class="text-left">${pageInfo.firstPage}</td>
                </tr>
                <tr>
                    <th  class="text-right" >前一页</th>
                    <td  class="text-left">${pageInfo.prePage}</td>
                </tr>
                <tr>
                    <th  class="text-right" >下一页</th>
                    <td  class="text-left">${pageInfo.nextPage}</td>
                </tr>
                <tr>
                    <th  class="text-right" >最后一页</th>
                    <td  class="text-left">${pageInfo.lastPage}</td>
                </tr>
                <tr>
                    <th  class="text-right" >是否为第一页</th>
                    <td  class="text-left">${pageInfo.isFirstPage?c}</td>
                </tr>
                <tr>
                    <th  class="text-right" >是否为最后一页</th>
                    <td  class="text-left">${pageInfo.isLastPage?c}</td>
                </tr>
                <tr>
                    <th  class="text-right" >是否有前一页</th>
                    <td  class="text-left">${pageInfo.hasPreviousPage?c}</td>
                </tr>
                <tr>
                    <th  class="text-right" >是否有下一页</th>
                    <td  class="text-left">${pageInfo.hasNextPage?c}</td>
                </tr>
            </table>
        </div>

    <#--

            <table class="gridtable" style="width:100%;">
                <#if msg??>
                    <tr style="color:red;">
                        <th colspan="5">${msg}</th>
                    </tr>
                </#if>
            </table>
    -->



        <h3 class="hidden text-success bg-info text-center" style="margin-bottom: 0px;">-= 查询结果 =-[<a href="${request.contextPath}/bills/add">新增支付订单</a>]</h3>



        <h4 class="hidden text-success bg-info text-center" style="margin-bottom: 0px;">[<a href="${request.contextPath}/bills/add">新增支付订单</a>]</h4>

        <table class="table table-bordered table-hover table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
            <thead>
            <tr style="background-color: #337ab7;color:white;"  class="bg-info">
                <th  style="width: 1%;" class="text-center" >序</th>
                <th  style="width: 2%;" class="text-center" >ID</th>
                <th  style="width: 5%;" class="text-center" >API_KEY</th>
                <th  style="width: 5%;" class="text-center" >API_MEMBERID</th>
                <th  style="width: 4%;" class="text-center" >API_AMOUNT</th>
                <th  style="width: 9%;" class="text-center" >API_ORDER_ID</th>
                <th  style="width: 8%;" class="text-center" >API_CHANNEL_BANK_NAME</th>
                <th  style="width: 4%;" class="text-center" >API_TIME_OUT</th>
                <th  style="width: 5%;" class="text-center" >API_ORDER_STATE</th>
                <th  style="width: 6%;" class="text-center" >API_NOTIFY_URL_PREFIX</th>
                <th  style="width: 5%;" class="text-center" >API_ORDER_TIME</th>
                <th  style="width: 5%;" class="text-center">操作</th>
            </tr>
            </thead>
            <tbody>
                <#list pageInfo.list as bill>
               <tr>
                    <td>${bill_index!}</td>
                    <td>${bill.id!?c}</td>
                    <td style="text-overflow: ellipsis">${bill.API_KEY!}</td>
                    <td>${bill.API_MEMBERID!}</td>
                    <td>${(((bill.API_AMOUNT?c)?number)/100)?string(',##0.00')}</td>
                    <td  class="text-left">${bill.API_ORDER_ID!}</td>
                    <td>${bill.API_CHANNEL_BANK_NAME!}</td>
                    <td>${bill.API_TIME_OUT!}</td>
                    <td>${bill.API_ORDER_STATE!}</td>
                    <td>${bill.API_NOTIFY_URL_PREFIX!}</td>
                    <td>${bill.API_ORDER_TIME?string('yyyy-MM-dd HH:mm:ss')}</td>
                    <td style="text-align:center;">[<a
                            href="${request.contextPath}/bills/view/${bill.id?c}">修改</a>] -
                        [<a href="${request.contextPath}/bills/delete/${bill.id?c}">删除</a>]
                    </td>
                </tr>
                </#list>
            </tbody>
        </table>





        <nav class="text-right" aria-label="Page navigation">
            <ul class="pagination">
                <#if pageInfo.hasPreviousPage>
                    <li>
                        <a href="${request.contextPath}/bills?page=1&rows=${pageInfo.pageSize!?c}&API_ORDER_ID=${queryParam.API_ORDER_ID}&API_CHANNEL_BANK_NAME=${queryParam.API_CHANNEL_BANK_NAME}">首页</a>
                    </li>
                    <li>
                        <a href="${request.contextPath}/bills?page=${pageInfo.prePage!?c}&rows=${pageInfo.pageSize!?c}&API_ORDER_ID=${queryParam.API_ORDER_ID}&API_CHANNEL_BANK_NAME=${queryParam.API_CHANNEL_BANK_NAME}">前一页</a>
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
                            <a href="${request.contextPath}/bills?page=${nav!?c}&rows=${pageInfo.pageSize!?c}&API_ORDER_ID=<#if queryParam.API_ORDER_ID??>${queryParam.API_ORDER_ID}</#if>&API_CHANNEL_BANK_NAME=<#if queryParam.API_CHANNEL_BANK_NAME??>${queryParam.API_CHANNEL_BANK_NAME}</#if>">${nav!?c}</a>
                        </li>
                    </#if>
                </#list>
                <#if pageInfo.hasNextPage>
                    <li>
                        <a href="${request.contextPath}/bills?page=${pageInfo.nextPage!?c}&rows=${pageInfo.pageSize!?c}&API_ORDER_ID=<#if queryParam.API_ORDER_ID??>${queryParam.API_ORDER_ID}</#if>&API_CHANNEL_BANK_NAME=<#if queryParam.API_CHANNEL_BANK_NAME??>${queryParam.API_CHANNEL_BANK_NAME}</#if>">下一页</a>
                    </li>
                    <li>
                        <a href="${request.contextPath}/bills?page=${pageInfo.pages!?c}&rows=${pageInfo.pageSize!?c}&API_ORDER_ID=<#if queryParam.API_ORDER_ID??>${queryParam.API_ORDER_ID}</#if>&API_CHANNEL_BANK_NAME=<#if queryParam.API_CHANNEL_BANK_NAME??>${queryParam.API_CHANNEL_BANK_NAME}</#if>">尾页</a>
                    </li>
                </#if>
                </li>
            </ul>
        </nav>
    </#if>
    </div>
    <div class="push"></div>
</div>
</body>
</html>