<!DOCTYPE html>
<html>
<head>
    <title>请求代付流水表</title>
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
    </style>
    <script>
        $(function () {
            $("#navLi li").removeClass("active")
            $("#qqdf").addClass("active")
            //展现错误消息框
            openDig = function openDig(channelName,channelCName,msg) {
                $("#diglogModalTitle").html("<b>[ESC]</b>："+channelName+" :[ "+channelCName+" ]"); //设置标题
                $("#diglogMain").text(msg); //设置内容
                $('#echartsModal').modal({ //展现模态框
                    keyboard: true
                })
            }
        });
    </script>
</head>
<body>
<#--页头-->
<#include "../../commons/header.ftl">

<div class="container-fluid">
    <div>
        <h1 class="hidden text-center"><a>请求代付流水表</a></h1>
        <form  class="form-inline" action="${request.contextPath}/reqDaifuList" method="post">
            <table class="table-bordered table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
                <tr>
                    <th class="text-right">订单号：</th>
                    <td>
                        <input  style="width: 100%;" class="form-control input-sm" type="text" name="orderId" value="<#if queryParam.orderId??>${queryParam.orderId}</#if>"/>
                    </td>
                    <th class="text-right">支付通道：</th>
                    <td>
                        <input style="width: 100%;"  type="text" class="form-control input-sm" name="channel" value="<#if queryParam.channel??>${queryParam.channel}</#if>"/>
                    </td>
                    <th class="text-right">商户号：</th>
                    <td>
                        <input style="width: 100%;"  type="text" class="form-control input-sm" name="channelMemberId" value="<#if queryParam.channelMemberId??>${queryParam.channelMemberId}</#if>"/>
                    </td>

                    <th class="text-right" style="width: 5%;">ServerID：</th>
                    <td style="width: 8%;">
                        <input style="width: 100%;"  type="text" class="form-control input-sm" name="serverId" value="<#if queryParam.serverId??>${queryParam.serverId}</#if>"/>
                    </td>

                    <td rowspan="2" style="width: 9%;">
                        <button type="submit" class="btn btn-success center-block" ><span class="glyphicon glyphicon-send"></span> 查询</button>
                    </td>
                </tr>
                <tr>
                    <td style="font-weight: bold; text-align: right;">
                        页码：  <input style="width: 50%;"  type="text" class="form-control input-sm" name="page" value="${(page!0)?c}"/>
                    </td>
                    <td style="font-weight: bold; text-align: right;">
                        页面大小：<input style="width: 50%;"  type="text" class="form-control input-sm" name="rows" value="${(rows!10)?c}"/>
                    </td>
                    <th class="text-right">业主：</th>
                    <td>
                        <select id="oid" name="oid" class="selectpicker show-menu-arrow form-control"  style="width: 100%;" >
                            <#if oidMaps?exists>
                                 <option  <#if  !queryParam.oid?? || queryParam.oid=="ALL" >SELECTED</#if>   value="ALL">全部</option>
                                 <#list oidMaps?keys as key>
                                  <option <#if  queryParam.oid?? && queryParam.oid==key>SELECTED </#if>  value="${key}">${oidMaps[key]}</option>
                                 </#list>
                            </#if>
                        </select>
                    </td>

                    <th  class="text-right">处理结果：</th>
                    <td style="font-weight: bold; text-align: left;" >
                        <select id="result" name="result" class="selectpicker show-menu-arrow form-control"  style="width: 100%;" >
                            <#if searchResultMaps?exists>
                                <option  <#if  !queryParam.result?? || queryParam.result=="ALL" >SELECTED</#if>   value="ALL">全部</option>
                                 <#list searchResultMaps?keys as key>
                                  <option <#if  queryParam.result?? && queryParam.result==key>SELECTED </#if>  value="${key}">${searchResultMaps[key]}</option>
                                 </#list>
                            </#if>
                        </select>
                    </td>

                    <th class="text-right" style="width: 5%;">客户IP：</th>
                    <td style="width: 8%;">
                        <input style="width: 100%;"  type="text" class="form-control input-sm" name="clientIp" value="<#if queryParam.clientIp??>${queryParam.clientIp}</#if>"/>
                    </td>


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
                    <h4 class="panel-title">
                       <#if ShowApiAmount?? && allAmount??> 总金额：${((allAmount?number)/100)?string(',##0.00')} 元, </#if>  共${pageInfo.total}条。
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



    <h3 class="hidden text-success bg-info text-center" style="margin-bottom: 0px;">-= 查询结果 =-<#-- [<a href="${request.contextPath}/reqDaifuList/add">新增</a>]--></h3>

        <table class="table table-bordered table-hover table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
            <thead>
            <tr style="background-color: #ffa07a;color:black;" class="bg-info">
                <th style="width: 1%;" class="text-center">序</th>
                <#if ShowIdAndOid??>
                    <th style="width: 3%;" class="text-center">ID</th>
                    <th style="width: 2%;" class="text-center">OID</th>
                <#else>
                    <th style="width: 4%;" class="text-center">业主</th>
                </#if>

                <th style="width: 7%;" class="text-center">订单号</th>
                <#if ShowApiAmount??> <th style="width: 3%;" class="text-center">金额</th></#if>
                <th style="width: 11%;" class="text-center">支付通道</th>
                <th style="width: 2%;" class="text-center">通道</th>
                <th style="width: 2%;" class="text-center">DB</th>
                <#if ShowMemberID??>
                <th style="width: 5%;" class="text-center">商户号</th>
                </#if>
                <th style="width: 7%;" class="text-center">通道名称</th>
                <th style="width: 6%;" class="text-center">时间</th>
                <th style="width: 3%;" class="text-center">请求</th>
                <th style="width: 4%;" class="text-center">订单状态</th>
            <#--<th style="width: 6%;" class="text-center">请求支付信息</th>-->
                <th style="width: 7%;" class="text-center">错误消息</th>
                <th style="width: 7%;" class="text-center">请求服务器</th>
                <th style="width: 4%;" class="text-center">操作</th>
            </tr>
            </thead>
            <tbody>
                <#list pageInfo.list as reqDaifuList>
               <#-- <#if reqDaifuList.result =="SUCCESS"><tr class="success"><#else> <tr class="danger"></#if>-->
                <#if reqDaifuList.orderStatus?? && ( reqDaifuList.orderStatus =="PAYING"|| reqDaifuList.orderStatus =="SUCCESS")><tr class="success"><#else> <tr class="danger"></#if>


                <#--<tr class="success">-->
                    <td>${reqPayList_index!}</td>
                    <#if ShowIdAndOid??>
                        <td>${reqDaifuList.id!?c}</td>
                        <td>${reqDaifuList.oid!}</td>
                    <#else>
                        <td>  <#if oidMaps?exists> ${oidMaps[reqDaifuList.oid!]!} </#if>  </td>
                    </#if>
                    <td>${reqDaifuList.orderId!}</td>
                    <#if ShowApiAmount??> <td class="text-right"><#if reqDaifuList.amount??> ${((reqDaifuList.amount?number)/100)?string(',##0.00')}<#--元--><#else> </#if></td> </#if>
                    <td class="text-left">${reqDaifuList.channel!}</td>
                    <td class="text-right">${reqDaifuList.requestDaifuResult.requestDaifuChannelTime!}</td>
                    <td class="text-right">${reqDaifuList.requestDaifuResult.requestDaifuGetReqDaifuInfoTime!}</td>
                    <#if ShowMemberID??> <td  class="text-left">${reqDaifuList.channelMemberId!}</td> </#if>
                    <td  class="text-left">${reqDaifuList.channelCName!}</td>
                    <td>${reqDaifuList.timeStmp?string('yyyy-MM-dd HH:mm:ss')}</td>
                    <td>
                         <#if reqDaifuList.result?? && 'SUCCESS'==reqDaifuList.result >成功<#elseif reqDaifuList.result?? && 'ERROR'==reqDaifuList.result >失败<#else> </#if>
                    </td>

                    <td>
                        <#-- ${reqDaifuList.requestDaifuResult.requestDaifuOrderState!}-->
                         ${reqDaifuList.orderStatus!}
                    </td>


                <#--<td>${reqDaifuList.reqPayInfo!}</td>-->
                    <td>
                        <a onclick="openDig('${reqDaifuList.channel!}','${reqDaifuList.channelCName!}','${reqDaifuList.requestDaifuResult.requestDaifuErrorMsg!?html?js_string}')" ref="#">  ${reqDaifuList.requestDaifuResult.requestDaifuErrorMsg!?html} </a>
                    </td>
                    <td class="text-left">${reqDaifuList.serverId!}</td>

                    <td style="text-align:center;">[<a href="${request.contextPath}/reqDaifuList/view/${reqDaifuList.id?c}">详情</a>]
                    <#if ShowReqDaifuListDelete?? >- [<a href="${request.contextPath}/reqDaifuList/delete/${reqDaifuList.id?c}">删除</a>] </#if>
                    </td>
                </tr>
                </#list>
            </tbody>
        </table>




        <nav class="text-right" aria-label="Page navigation">
            <ul class="pagination">
                <#if pageInfo.hasPreviousPage>
                    <li>
                        <a href="${request.contextPath}/reqDaifuList?page=1&rows=${pageInfo.pageSize!?c}&orderId=${queryParam.orderId}&oid=${queryParam.oid}&channel=${queryParam.channel}&channelMemberId=${queryParam.channelMemberId}&result=${queryParam.result}&serverId=${queryParam.serverId}&clientIp=${queryParam.clientIp}">首页</a>
                    </li>
                    <li>
                        <a href="${request.contextPath}/reqDaifuList?page=${pageInfo.prePage!?c}&rows=${pageInfo.pageSize!?c}&orderId=${queryParam.orderId}&oid=${queryParam.oid}&channel=${queryParam.channel}&channelMemberId=${queryParam.channelMemberId}&result=${queryParam.result}&serverId=${queryParam.serverId}&clientIp=${queryParam.clientIp}">前一页</a>
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
                            <a href="${request.contextPath}/reqDaifuList?page=${nav!?c}&rows=${pageInfo.pageSize!?c}&orderId=<#if queryParam.orderId??>${queryParam.orderId}</#if>&oid=<#if queryParam.oid??>${queryParam.oid}</#if>&channel=<#if queryParam.channel??>${queryParam.channel}</#if>&channelMemberId=<#if queryParam.channelMemberId??>${queryParam.channelMemberId}</#if>&result=<#if queryParam.result??>${queryParam.result}</#if>&serverId=<#if queryParam.serverId??>${queryParam.serverId}</#if>&clientIp=<#if queryParam.clientIp??>${queryParam.clientIp}</#if>">${nav!?c}</a>
                        </li>
                    </#if>
                </#list>
                <#if pageInfo.hasNextPage>
                    <li>
                        <a href="${request.contextPath}/reqDaifuList?page=${pageInfo.nextPage!?c}&rows=${pageInfo.pageSize!?c}&orderId=<#if queryParam.orderId??>${queryParam.orderId}</#if>&oid=<#if queryParam.oid??>${queryParam.oid}</#if>&channel=<#if queryParam.channel??>${queryParam.channel}</#if>&channelMemberId=<#if queryParam.channelMemberId??>${queryParam.channelMemberId}</#if>&result=<#if queryParam.result??>${queryParam.result}</#if>&serverId=<#if queryParam.serverId??>${queryParam.serverId}&clientIp=<#if queryParam.clientIp??>${queryParam.clientIp}</#if></#if>">下一页</a>
                    </li>
                    <li>
                        <a href="${request.contextPath}/reqDaifuList?page=${pageInfo.pages!?c}&rows=${pageInfo.pageSize!?c}&orderId=<#if queryParam.orderId??>${queryParam.orderId}</#if>&oid=<#if queryParam.oid??>${queryParam.oid}</#if>&channel=<#if queryParam.channel??>${queryParam.channel}</#if>&channelMemberId=<#if queryParam.channelMemberId??>${queryParam.channelMemberId}</#if>&result=<#if queryParam.result??>${queryParam.result}</#if>&serverId=<#if queryParam.serverId??>${queryParam.serverId}</#if>&clientIp=<#if queryParam.clientIp??>${queryParam.clientIp}</#if>">尾页</a>
                    </li>
                </#if>
                </li>
            </ul>
        </nav>
    </#if>
    </div>

    <div class="push">

        <!-- 模态框（Modal） -->
        <div class="modal fade" id="echartsModal" tabindex="-1" aria-hidden="true" role="dialog" aria-labelledby="echartsModalTitle" aria-hidden="true">
            <div class="modal-dialog" style="width:1000px">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="diglogModalTitle">   </h4>
                    </div>
                    <div class="modal-body">
                        <div style="font-size: 18px;text-align: initial;word-wrap: break-word; word-break: normal;"  class="text-primary text-justify h3"  id="diglogMain" overflow:auto;"></div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" data-dismiss="modal">关闭</button>
                </div>
            </div><!-- /.modal-content -->
        </div><!-- /.modal -->
    </div>
</div>


</div>


</div>
</body>
</html>