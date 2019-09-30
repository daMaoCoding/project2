<html>
<head>
    <title>响应支付流水表</title>
    <link rel="shortcut icon" href="${request.contextPath}/static/favicon.ico">
    <link href="${request.contextPath}/static/css/style_pay.css" rel="stylesheet" type="text/css"/>
    <script src="${request.contextPath}/static/js/jquery-1.11.1.min.js"></script>
    <script>
        $(function () {
            $("#navLi li").removeClass("active")
            $("#xyzf").addClass("active")
        });
    </script>
</head>
<body style="margin-top:50px;">
<#--页头-->
<#include "../../commons/header.ftl">
<form style="position: absolute; top: 55px;" action="${request.contextPath}/resPayList/save" method="post">
    <input type="hidden" name="id" value="<#if resPayList.id??>${resPayList.id}</#if>"/>
    <table class="gridtable" style="width:95%;">
        <tr>
            <th style="text-align: center;" colspan="4">响应支付流水表信息 - [<a href="${request.contextPath}/resPayList">返回</a>]</th>
        </tr>
        <tr>
            <th style="width: 120px;">订单号：</th>
            <td>
                <input  type="text" name="orderId" value="<#if resPayList.orderId??>${resPayList.orderId}</#if>"/>
            </td>
            <#if ShowApiAmount??>
            <th style="width: 120px;">金额(元)：</th>
            <td>
                <input type="text" name="amount" value="<#if resPayList.amount??>${((resPayList.amount?number)/100)?string(',##0.00')}</#if>"/>
            </td>
            </#if>

        </tr>
        <tr>
            <th>支付通道：</th>
            <td>
                <input  type="text" name="channel" value="<#if resPayList.channel??>${resPayList.channel}</#if>"/>
            </td>

            <#if ShowMemberID??>
            <th>商户号：</th>
            <td>
                <input  type="text" name="channelMemberId" value="<#if resPayList.channelMemberId??>${resPayList.channelMemberId}</#if>"/>
            </td>
            </#if>

        </tr>
        <tr>

            <th>响应时间：</th>
            <td>
                 <input  type="text" name="timeStmp" value="<#if resPayList.timeStmp??>${resPayList.timeStmp?string('yyyy-MM-dd hh:mm:ss')}</#if>"/>
            </td>


            <th>处理结果：</th>
            <td>
                <input  type="text" name="result" value="<#if resPayList.result??>${resPayList.result}</#if>"/>
            </td>
        </tr>


        <tr>
            <th>响应支付结果(给DB)：</th>
            <td colspan="3">
            <#if resPayList.responsePayResult??>
                <table style="table-layout: fixed; width: 100%; ">
                    <tr>
                        <th style="width: 200px;text-align: left;">内存地址：</th>
                        <th style="text-align: left;"> ${resPayList.responsePayResult} </th>
                    </tr>



                    <tr>
                        <th style="text-align: left;">总处理结果：</th>
                        <td style="word-wrap:break-word;">
                        ${resPayList.responsePayResult.responsePayCode!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">订单号：</th>
                        <td style="word-wrap:break-word;">
                        ${resPayList.responsePayResult.responseOrderID!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">第三方IP：</th>
                        <td style="word-wrap:break-word;">
                             <#if resPayList??  &&  resPayList.resPayRemoteIp??>${resPayList.resPayRemoteIp!} - ${IpHelperCZ.findStrAddress(resPayList.resPayRemoteIp!)!}<#else> ...  </#if>
                        </td>
                    </tr>


                    <tr>
                        <th style="text-align: left;">下单IP：</th>
                        <td style="word-wrap:break-word;">
                             <#if resPayList?? && resPayList.reqPayInfo?? &&  resPayList.reqPayInfo.API_Client_IP??>${resPayList.reqPayInfo.API_Client_IP!} - ${IpHelperCZ.findStrAddress(resPayList.reqPayInfo.API_Client_IP!)!}<#else> ...  </#if>
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">订单时间：</th>
                        <td style="word-wrap:break-word;">
                             <#if resPayList.reqPayTimeStmp??>${resPayList.reqPayTimeStmp?string('yyyy-MM-dd HH:mm:ss')}</#if>
                        </td>
                    </tr>

                      <tr>
                        <th style="text-align: left;">回调时间：</th>
                        <td style="word-wrap:break-word;">
                            <#if resPayList.timeStmp??>${resPayList.timeStmp?string('yyyy-MM-dd HH:mm:ss')}</#if>
                        </td>
                    </tr>


                    <tr>
                        <th style="text-align: left;">订单状态：</th>
                        <td style="word-wrap:break-word;">
                        ${resPayList.responsePayResult.responseOrderState!}
                        </td>
                    </tr>
                    <tr>
                        <th style="text-align: left;">通道：</th>
                        <td style="word-wrap:break-word;">
                        ${resPayList.responsePayResult.responsePayChannel!}
                        </td>
                    </tr>

                     <#if ShowApiAmount??>
                    <tr>
                        <th style="text-align: left;">金额：</th>
                        <td style="word-wrap:break-word;">
                        ${resPayList.responsePayResult.responsePayAmount!}
                        </td>
                    </tr>
                     </#if>

                 <#if ShowMemberID??>
                    <tr>
                        <th style="text-align: left;">商户号：</th>
                        <td style="word-wrap:break-word;">
                        ${resPayList.responsePayResult.responsePayMemberId!}
                        </td>
                    </tr>
                 </#if>


                    <tr>
                        <th style="text-align: left;">错误消息：</th>
                        <td style="word-wrap:break-word;">
                        ${resPayList.responsePayResult.responsePayErrorMsg!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">返回消息：</th>
                        <td style="word-wrap:break-word;">
                        ${resPayList.responsePayResult.responsePayMsg!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">耗时：</th>
                        <td style="word-wrap:break-word;">
                        ${resPayList.responsePayResult.responsePayTotalTime!}
                        </td>
                    </tr>
                    <tr>
                        <th style="text-align: left;">自定义参数：</th>
                        <td style="word-wrap:break-word;">
                            ${resPayList.responsePayResult.responsePayOtherParam!}
                        </td>
                    </tr>

                <#if  ShowIdAndOid??>

                    <tr>
                        <th style="text-align: left;">OID：</th>
                        <td style="word-wrap:break-word;">
                            ${resPayList.responsePayResult.responsePayOid!}
                        </td>
                    </tr>
                </#if>

                    <tr>
                        <th style="text-align: left;">通知DB签名：</th>
                        <td style="word-wrap:break-word;">
                            ${resPayList.responsePayResult.responsePaySign!}
                        </td>
                    </tr>
                    <tr>
                        <th style="text-align: left;">通知DB次数：</th>
                        <td style="word-wrap:break-word;">
                            ${resPayList.resDbCount!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">通知DB结果：</th>
                        <td style="word-wrap:break-word;">
                            ${resPayList.resDbResult!}
                        </td>
                    </tr>
                    <tr>
                        <th style="text-align: left;">DB返回信息：</th>
                        <td style="word-wrap:break-word;">
                            ${resPayList.resDbMsg!}
                        </td>
                    </tr>
                </table>
            </#if>
            </td>
        </tr>


        <tr>
            <th>响应支付参数：</th>
            <td colspan="3">
                <#if resPayList.responsePayParams??>
                    <table style="table-layout: fixed; width: 100%; ">
                       <tr>
                            <th style="width: 250px;text-align: left;">参数个数（来自第三方）：</th>
                            <th style="text-align: left;">${resPayList.responsePayParams?size}</th>
                       </tr>
                        <#list resPayList.responsePayParams as key, value>
                            <tr>
                                <th style="text-align: left;"> ${key} </th>
                                <td style="word-wrap:break-word;"> ${value?html} </td>
                            </tr>
                        </#list>
                        <#--<#list resPayList.responsePayParams?keys as key>-->
                            <#--<tr>-->
                                <#--<td style="text-align: left;"> ${key} </td>-->
                                <#--<td style="word-wrap:break-word;"> ${resPayList.responsePayParams[key]?html} </td>-->
                            <#--</tr>-->
                        <#--</#list>-->
                    </table>
                </#if>
            </td>
        </tr>

<#--
        <tr>
            <td colspan="4"><input type="submit" value="保存"/></td>
        </tr>
-->

    <#if msg??>
        <tr style="color:#00ba00;">
            <th colspan="5">${msg}</th>
        </tr>
    </#if>
    </table>
</form>
</body>
</html>
