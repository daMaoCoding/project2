<html>
<head>
    <title>响应代付流水表</title>
    <link rel="shortcut icon" href="${request.contextPath}/static/favicon.ico">
    <link href="${request.contextPath}/static/css/style_pay.css" rel="stylesheet" type="text/css"/>
    <script src="${request.contextPath}/static/js/jquery-1.11.1.min.js"></script>
    <script>
        $(function () {
            $("#navLi li").removeClass("active")
            $("#xydf").addClass("active")
        });
    </script>
</head>
<body style="margin-top:50px;">
<#--页头-->
<#include "../../commons/header.ftl">
<form style="position: absolute; top: 55px;" action="${request.contextPath}/resDaifuList/save" method="post">
    <input type="hidden" name="id" value="<#if resDaifuList.id??>${resDaifuList.id}</#if>"/>
    <table class="gridtable" style="width:95%;">
        <tr>
            <th style="text-align: center;" colspan="4">响应代付流水表信息 - [<a href="${request.contextPath}/resDaifuList">返回</a>]</th>
        </tr>
        <tr>
            <th style="width: 10%;">订单号：</th>
            <td>
                <input  type="text" name="orderId" value="<#if resDaifuList.orderId??>${resDaifuList.orderId}</#if>"/>
            </td>
            <#if ShowApiAmount??>
            <th style="width: 120px;">金额(元)：</th>
            <td>
                <input type="text" name="amount" value="<#if resDaifuList.amount??>${((resDaifuList.amount?number)/100)?string(',##0.00')}</#if>"/>
            </td>
            </#if>

        </tr>
        <tr>
            <th>支付通道：</th>
            <td>
                <input  type="text" name="channel" value="<#if resDaifuList.channel??>${resDaifuList.channel}</#if>"/>
            </td>

            <#if ShowMemberID??>
            <th>商户号：</th>
            <td>
                <input  type="text" name="channelMemberId" value="<#if resDaifuList.channelMemberId??>${resDaifuList.channelMemberId}</#if>"/>
            </td>
            </#if>

        </tr>
        <tr>

            <th>响应时间：</th>
            <td>
                 <input  type="text" name="timeStmp" value="<#if resDaifuList.timeStmp??>${resDaifuList.timeStmp?string('yyyy-MM-dd hh:mm:ss')}</#if>"/>
            </td>


            <th>处理结果：</th>
            <td>
                <input  type="text" name="result" value="<#if resDaifuList.result??>${resDaifuList.result}</#if>"/>
            </td>
        </tr>


        <tr>
            <th>响应代付结果(给DB)：</th>
            <td colspan="3">
            <#if resDaifuList.responseDaifuResult??>
                <table style="table-layout: fixed; width: 100%; ">
                    <tr>
                        <th style="width: 200px;text-align: left;">内存地址：</th>
                        <th style="text-align: left;"> ${resDaifuList.responseDaifuResult} </th>
                    </tr>



                    <tr>
                        <th  style="text-align: left;background: lightsteelblue;">总处理结果：</th>
                        <td style="word-wrap:break-word;">
                        ${resDaifuList.responseDaifuResult.responseDaifuCode!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;background: lightsteelblue;">订单号：</th>
                        <td style="word-wrap:break-word;">
                        ${resDaifuList.responseDaifuResult.responseOrderID!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">第三方IP：</th>
                        <td style="word-wrap:break-word;">
                             <#if resDaifuList??  &&  resDaifuList.resDaifuRemoteIp??>${resDaifuList.resDaifuRemoteIp!} - ${IpHelperCZ.findStrAddress(resDaifuList.resDaifuRemoteIp!)!}<#else> ...  </#if>
                        </td>
                    </tr>


                    <tr>
                        <th style="text-align: left;">下单IP：</th>
                        <td style="word-wrap:break-word;">
                             <#if resDaifuList?? && resDaifuList.reqDaifuInfo?? &&  resDaifuList.reqDaifuInfo.API_Client_IP??>${resDaifuList.reqDaifuInfo.API_Client_IP!} - ${IpHelperCZ.findStrAddress(resDaifuList.reqDaifuInfo.API_Client_IP!)!}<#else> ...  </#if>
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">订单时间：</th>
                        <td style="word-wrap:break-word;">
                             <#if resDaifuList.reqDaifuTimeStmp??>${resDaifuList.reqDaifuTimeStmp?string('yyyy-MM-dd HH:mm:ss')}</#if>
                        </td>
                    </tr>

                      <tr>
                        <th style="text-align: left;">回调时间：</th>
                        <td style="word-wrap:break-word;">
                            <#if resDaifuList.timeStmp??>${resDaifuList.timeStmp?string('yyyy-MM-dd HH:mm:ss')}</#if>
                        </td>
                    </tr>


                    <tr>
                        <th style="text-align: left;background: lightsalmon;">订单状态：</th>
                        <td style="word-wrap:break-word;">
                        ${resDaifuList.responseDaifuResult.responseOrderState!}
                        </td>
                    </tr>
                    <tr>
                        <th style="text-align: left;">通道：</th>
                        <td style="word-wrap:break-word;">
                        ${resDaifuList.responseDaifuResult.responseDaifuChannel!}
                        </td>
                    </tr>

                     <#if ShowApiAmount??>
                    <tr>
                        <th style="text-align: left;">金额：</th>
                        <td style="word-wrap:break-word;">
                        ${resDaifuList.responseDaifuResult.responseDaifuAmount!}
                        </td>
                    </tr>
                     </#if>

                 <#if ShowMemberID??>
                    <tr>
                        <th style="text-align: left;">商户号：</th>
                        <td style="word-wrap:break-word;">
                        ${resDaifuList.responseDaifuResult.responseDaifuMemberId!}
                        </td>
                    </tr>
                 </#if>

                    <#if resDaifuList.reqDaifuInfo??>
                        <tr>
                            <th style="text-align: left;background: lightsalmon;">会员账号：</th>
                            <td style="word-wrap:break-word;">
                               <#if resDaifuList.reqDaifuInfo??  &&  resDaifuList.reqDaifuInfo.API_CUSTOMER_ACCOUNT??>  ${resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_ACCOUNT()!}</#if>
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;background: lightsalmon;">会员姓名：</th>
                            <td style="word-wrap:break-word;">
                               <#if resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_NAME??>  ${resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_NAME()!}</#if>
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;background: lightsalmon;">银行名称：</th>
                            <td style="word-wrap:break-word;">
                                <#if resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_BANK_NAME()??>  ${resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_BANK_NAME()!}</#if>
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;background: lightsalmon;">分行所在省份：</th>
                            <td style="word-wrap:break-word;">
                                <#if  resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_BANK_BRANCH??>  ${resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_BANK_BRANCH()!}</#if>
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;background: lightsalmon;">支行所在城市：</th>
                            <td style="word-wrap:break-word;">
                                <#if  resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_BANK_SUB_BRANCH??>  ${resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_BANK_SUB_BRANCH()!}</#if>
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;background: lightsalmon;">银行账号：</th>
                            <td style="word-wrap:break-word;">
                                <#if  resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_BANK_NUMBER??>  ${resDaifuList.reqDaifuInfo.getAPI_CUSTOMER_BANK_NUMBER()!}</#if>
                            </td>
                        </tr>
                    </#if>



                    <tr>
                        <th style="text-align: left;background: lightsteelblue;">错误消息：</th>
                        <td style="word-wrap:break-word;">
                        ${resDaifuList.responseDaifuResult.responseDaifuErrorMsg!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">返回消息：</th>
                        <td style="word-wrap:break-word;">
                        ${resDaifuList.responseDaifuResult.responseDaifuMsg!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">耗时：</th>
                        <td style="word-wrap:break-word;">
                        ${resDaifuList.responseDaifuResult.responseDaifuTotalTime!}
                        </td>
                    </tr>
                    <tr>
                        <th style="text-align: left;">自定义参数：</th>
                        <td style="word-wrap:break-word;">
                            ${resDaifuList.responseDaifuResult.responseDaifuOtherParam!}
                        </td>
                    </tr>

                <#if  ShowIdAndOid??>

                    <tr>
                        <th style="text-align: left;">OID：</th>
                        <td style="word-wrap:break-word;">
                            ${resDaifuList.responseDaifuResult.responseDaifuOid!}
                        </td>
                    </tr>
                </#if>

                    <tr>
                        <th style="text-align: left;">通知DB签名：</th>
                        <td style="word-wrap:break-word;">
                            ${resDaifuList.responseDaifuResult.responseDaifuSign!}
                        </td>
                    </tr>
                    <tr>
                        <th style="text-align: left;">通知DB次数：</th>
                        <td style="word-wrap:break-word;">
                            ${resDaifuList.resDbCount!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;background: lightsteelblue;">通知DB结果：</th>
                        <td style="word-wrap:break-word;">
                            ${resDaifuList.resDbResult!}
                        </td>
                    </tr>
                    <tr>
                        <th style="text-align: left;background: lightsteelblue;">DB返回信息：</th>
                        <td style="word-wrap:break-word;">
                            ${resDaifuList.resDbMsg!}
                        </td>
                    </tr>


                </table>
            </#if>
            </td>
        </tr>


        <tr>
            <th>响应代付参数：</th>
            <td colspan="3">
                <#if resDaifuList.responseDaifuParams??>
                    <table style="table-layout: fixed; width: 100%; ">
                       <tr>
                            <th style="width: 250px;text-align: left;">参数个数（来自第三方）：</th>
                            <th style="text-align: left;">${resDaifuList.responseDaifuParams?size}</th>
                       </tr>
                        <#list resDaifuList.responseDaifuParams as key, value>
                            <tr>
                                <th style="text-align: left;"> ${key} </th>
                                <td style="word-wrap:break-word;"> ${value?html} </td>
                            </tr>
                        </#list>
                        <#--<#list ResDaifulist.responsePayParams?keys as key>-->
                            <#--<tr>-->
                                <#--<td style="text-align: left;"> ${key} </td>-->
                                <#--<td style="word-wrap:break-word;"> ${ResDaifulist.responsePayParams[key]?html} </td>-->
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
