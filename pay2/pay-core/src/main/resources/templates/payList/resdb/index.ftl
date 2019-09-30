<!DOCTYPE html>
<html>
<head>
    <title>响应支付流水表</title>
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


    <!-- 弹出框 -->
    <script src="${request.contextPath}/static/toastr/toastr.min.js"></script>
    <link href="${request.contextPath}/static/toastr/toastr.min.css" rel="stylesheet" rel="stylesheet">


<#--    <link href="${request.contextPath}/static/css/style_pay.css" rel="stylesheet" type="text/css"/>-->
    <style type="text/css">
        td {white-space:nowrap;overflow:hidden;word-break:keep-all;text-overflow:ellipsis}
    </style>
    <script>
        $(function () {
            $("#navLi li").removeClass("active");
            $("#xyzf").addClass("active");
            //补发
            bufa=function (obj,orderid,url) {
                $(obj).removeAttr('onclick');
                $.post(url).done(function(data) { //请求数据
                    toastr.options = {
                        "closeButton": true,
                        "debug": false,
                        "newestOnTop": false,
                        "progressBar": true,
                        "positionClass": "toast-top-center",
                        "preventDuplicates": false,
                        "onclick": null,
                        "showDuration": "300",
                        "hideDuration": "2000",
                        "timeOut": "2000",
                        "extendedTimeOut": "2000",
                        "showEasing": "swing",
                        "hideEasing": "linear",
                        "showMethod": "fadeIn",
                        "hideMethod": "fadeOut"
                    }
                    toastr.options.onHidden = function() { $("#searchBtn").click(); }

                   if(data=='SUCCESS'){
                       toastr.success("补发成功，将刷新本页面。");
                   }else{
                       toastr.error("补发失败。")
                   }

                });

            };


        });
    </script>
</head>
<body>
<#--页头-->
<#include "../../commons/header.ftl">
<div class="container-fluid">
    <div>
       <#-- <h1 class="text-center">响应支付流水表</h1>-->

        <form  class="form-inline"  action="${request.contextPath}/resPayList" method="post">
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


                    <th class="text-right" style="width: 6%;">回调服务器：</th>
                    <td style="width: 8%;">
                        <input style="width: 100%;"  type="text" class="form-control input-sm" name="resPayRemoteIp" value="<#if queryParam.resPayRemoteIp??>${queryParam.resPayRemoteIp}</#if>"/>
                    </td>


                    <td rowspan="2" style="width: 9%;">
                        <button id="searchBtn" type="submit" class="btn btn-success center-block" ><span class="glyphicon glyphicon-send"></span> 查询</button>
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


                    <td style="font-weight: bold; text-align: right;">
                        处理结果：
                        <select id="result" name="result" class="selectpicker show-menu-arrow form-control"  style="width: 50%;" >
                            <#if searchResultMaps?exists>
                                <option  <#if  !queryParam.result?? || queryParam.result=="ALL" >SELECTED</#if>   value="ALL">全部</option>
                                 <#list searchResultMaps?keys as key>
                                  <option <#if  queryParam.result?? && queryParam.result==key>SELECTED </#if>  value="${key}">${searchResultMaps[key]}</option>
                                 </#list>
                            </#if>
                        </select>

                    </td>
                    <td style="font-weight: bold; text-align: right;">
                        DB通知结果：
                        <select id="resDbResult" name="resDbResult" class="selectpicker show-menu-arrow form-control"  style="width: 50%;" >
                            <#if searchResultMaps?exists>
                                <option  <#if  !queryParam.resDbResult?? || queryParam.resDbResult=="ALL" >SELECTED</#if>   value="ALL">全部</option>
                                 <#list searchResultMaps?keys as key>
                                  <option <#if  queryParam.resDbResult?? && queryParam.resDbResult==key>SELECTED </#if>  value="${key}">${searchResultMaps[key]}</option>
                                 </#list>
                            </#if>
                        </select>
                    </td>



                    <th class="text-right" style="width: 6%;">DB通知次数≥：</th>
                    <td style="width: 8%;">
                       <#-- <input style="width: 100%;"  type="text" class="form-control input-sm" name="resDbCount" value="<#if queryParam.resDbCount??>${queryParam.resDbCount}</#if>"/>-->
                        <select id="resDbCount" name="resDbCount" class="selectpicker show-menu-arrow form-control"  style="width: 50%;" >
                            <option  <#if  !queryParam.resDbCount?? || queryParam.resDbCount?number==0 >SELECTED </#if>   value="0">0</option>
                            <option  <#if  queryParam.resDbCount??  && queryParam.resDbCount?number==1 >SELECTED </#if>   value="1">1</option>
                            <option  <#if  queryParam.resDbCount??  && queryParam.resDbCount?number==2 >SELECTED </#if>   value="2">2</option>
                            <option  <#if  queryParam.resDbCount??  && queryParam.resDbCount?number==3 >SELECTED </#if>   value="3">3</option>
                            <option  <#if  queryParam.resDbCount??  && queryParam.resDbCount?number==4 >SELECTED </#if>   value="4">4</option>
                            <option  <#if  queryParam.resDbCount??  && queryParam.resDbCount?number==5 >SELECTED </#if>   value="5">5</option>
                            <option  <#if  queryParam.resDbCount??  && queryParam.resDbCount?number==6 >SELECTED </#if>   value="6">6</option>
                            <option  <#if  queryParam.resDbCount??  && queryParam.resDbCount?number==7 >SELECTED </#if>   value="7">7</option>
                            <option  <#if  queryParam.resDbCount??  && queryParam.resDbCount?number==8 >SELECTED </#if>   value="8">8</option>
                        </select>
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
                        <#if ShowApiAmount?? && allAmount??> 总金额：${((allAmount?number)/100)?string(',##0.00')} 元,</#if> 共${pageInfo.total}条。
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



       <#-- <h3 class="text-success bg-info text-center"  style="margin-bottom: 0px;">-= 查询结果 =-  <#-- [<a href="${request.contextPath}/reqPayList/add">新增</a>]--></h3>
        <table class="table table-bordered table-hover table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
            <thead>
            <tr style="background-color: #337ab7;color:white;"  class="bg-info">
                <th class="text-center" style="width: 1%;"  >序</th>
                 <#if  ShowIdAndOid??>
                    <th class="text-center" style="width: 3%;"  >ID</th>
                    <th class="text-center" style="width: 2%;"  >OID</th>
                 <#else>
                    <th class="text-center" style="width: 4%;"  >业主</th>
                 </#if>
                <th class="text-center" style="width: 6%;"  >订单号</th>
                <#if ShowApiAmount??>  <th class="text-center" style="width: 2%;"  >金额</th></#if>
                <th class="text-center" style="width: 8%;"  >支付通道</th>
                <#if  ShowMemberID??> <th class="text-center" style="width: 4%;"  >商户号</th> </#if>
                <th class="text-center" style="width: 6%;"  >通道名称</th>
                <th class="text-center" style="width: 5%;"  >时间</th>
                <th class="text-center" style="width: 6%;"  >请求服务器</th>
                <th class="text-center" style="width: 3%;" >回调服务器</th>
                <th class="text-center" style="width: 2%;"  >响应</th>
                <th class="text-center" style="width: 2%;"  >参数</th>
                <th class="text-center" style="width: 2%;"  >通知</th>
                <th class="text-center" style="width: 1%;"  >(次)</th>
                <th class="text-center" style="width: 4%;"  >操作</th>
            </tr>
            </thead>
            <tbody>
                <#list pageInfo.list as resPayList>
                <#if resPayList.result =="SUCCESS"><tr class="success"  id="${resPayList.id?c}"><#else> <tr id="${resPayList.id?c}" class="danger"></#if>
                <#--<tr class="success">-->
                    <td>${resPayList_index!}</td>
                    <#if  ShowIdAndOid??>
                    <td>${resPayList.id!?c}</td>
                    <td>${resPayList.oid!}</td>
                    <#else>
                         <td>  <#if oidMaps?exists> ${oidMaps[resPayList.oid!]!} </#if>  </td>
                    </#if>

                    <td>${resPayList.orderId!}</td>
                    <#if ShowApiAmount??>  <td   class="text-right"><#if resPayList.amount??> ${((resPayList.amount?number)/100)?string(',##0.00')}<#--元--><#else> </#if></td></#if>
                    <td  class="text-left">${resPayList.channel!}</td>
                    <#if ShowMemberID??>
                     <td  class="text-left">${resPayList.channelMemberId!}</td>
                    </#if>
                    <td  class="text-left">${resPayList.channelCName!}</td>
                    <td>${resPayList.timeStmp?string('yyyy-MM-dd HH:mm:ss')}</td>
                    <td class="text-left">${resPayList.serverId!}</td>
                    <td class="text-left"> <#if resPayList??  &&  resPayList.resPayRemoteIp??>${resPayList.resPayRemoteIp!}<#else></#if></td>
                    <td>
                       <#if resPayList.result?? && 'SUCCESS'==resPayList.result >成功<#elseif resPayList.result?? && 'ERROR'==resPayList.result >失败<#else> </#if>
                    </td>
                    <td>${resPayList.responsePayParams?size} (个)</td>
                    <#if resPayList.resDbResult! =="ERROR"><td class="danger"><#else> <td class="success"></#if>
                    <#if resPayList.resDbResult?? && 'SUCCESS'==resPayList.resDbResult >成功<#elseif resPayList.resDbResult?? && 'ERROR'==resPayList.resDbResult >失败<#else> </#if>
                    <td> ${resPayList.resDbCount!}  </td>
                    <td style="text-align:center;font-size: 13px;">
                        [<a  href="${request.contextPath}/resPayList/view/${resPayList.id?c}">详情</a>]
                       <#if resPayListDelete?? > - [<a href="${request.contextPath}/resPayList/delete/${resPayList.id?c}">删除</a>] </#if>
                       <#if resPayList.result?? && 'SUCCESS'==resPayList.result >
                           [<a onclick="bufa(this,'${resPayList.orderId!}','${request.contextPath}/resPayList/bufa/${resPayList.id?c}')" ref="#">补发</a>]
                       </#if>

                    </td>
                </td>
                </#list>
            </tbody>
        </table>

        <nav class="text-right" aria-label="Page navigation">
            <ul class="pagination">
                <#if pageInfo.hasPreviousPage>
                    <li>
                        <a href="${request.contextPath}/resPayList?page=1&rows=${pageInfo.pageSize!?c}&orderId=${queryParam.orderId}&oid=${queryParam.oid}&channel=${queryParam.channel}&channelMemberId=${queryParam.channelMemberId}&result=${queryParam.result}&resDbResult=${queryParam.resDbResult}&resPayRemoteIp=${queryParam.resPayRemoteIp}&resDbCount=${queryParam.resDbCount}">首页</a>
                    </li>
                    <li>
                        <a href="${request.contextPath}/resPayList?page=${pageInfo.prePage!?c}&rows=${pageInfo.pageSize!?c}&orderId=${queryParam.orderId}&oid=${queryParam.oid}&channel=${queryParam.channel}&channelMemberId=${queryParam.channelMemberId}&result=${queryParam.result}&resDbResult=${queryParam.resDbResult}&resPayRemoteIp=${queryParam.resPayRemoteIp}&resDbCount=${queryParam.resDbCount}">前一页</a>
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
                            <a href="${request.contextPath}/resPayList?page=${nav!?c}&rows=${pageInfo.pageSize!?c}&orderId=<#if queryParam.orderId??>${queryParam.orderId}</#if>&oid=<#if queryParam.oid??>${queryParam.oid}</#if>&channel=<#if queryParam.channel??>${queryParam.channel}</#if>&channelMemberId=<#if queryParam.channelMemberId??>${queryParam.channelMemberId}</#if>&result=<#if queryParam.result??>${queryParam.result}</#if>&resDbResult=<#if queryParam.resDbResult??>${queryParam.resDbResult}</#if>&resPayRemoteIp=<#if queryParam.resPayRemoteIp??>${queryParam.resPayRemoteIp}</#if>&resDbCount=<#if queryParam.resDbCount??>${queryParam.resDbCount}</#if>">${nav!?c}</a>
                        </li>
                    </#if>
                </#list>
                <#if pageInfo.hasNextPage>
                    <li>
                        <a href="${request.contextPath}/resPayList?page=${pageInfo.nextPage!?c}&rows=${pageInfo.pageSize!?c}&orderId=<#if queryParam.orderId??>${queryParam.orderId}</#if>&oid=<#if queryParam.oid??>${queryParam.oid}</#if>&channel=<#if queryParam.channel??>${queryParam.channel}</#if>&channelMemberId=<#if queryParam.channelMemberId??>${queryParam.channelMemberId}</#if>&result=<#if queryParam.result??>${queryParam.result}</#if>&resDbResult=<#if queryParam.resDbResult??>${queryParam.resDbResult}</#if>&resPayRemoteIp=<#if queryParam.resPayRemoteIp??>${queryParam.resPayRemoteIp}</#if>&resDbCount=<#if queryParam.resDbCount??>${queryParam.resDbCount}</#if>">下一页</a>
                    </li>
                    <li>
                        <a href="${request.contextPath}/resPayList?page=${pageInfo.pages!?c}&rows=${pageInfo.pageSize!?c}&orderId=<#if queryParam.orderId??>${queryParam.orderId}</#if>&oid=<#if queryParam.oid??>${queryParam.oid}</#if>&channel=<#if queryParam.channel??>${queryParam.channel}</#if>&channelMemberId=<#if queryParam.channelMemberId??>${queryParam.channelMemberId}</#if>&result=<#if queryParam.result??>${queryParam.result}</#if>&resDbResult=<#if queryParam.resDbResult??>${queryParam.resDbResult}</#if>&resPayRemoteIp=<#if queryParam.resPayRemoteIp??>${queryParam.resPayRemoteIp}</#if>&resDbCount=<#if queryParam.resDbCount??>${queryParam.resDbCount}</#if>">尾页</a>
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