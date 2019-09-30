<html>
<head>
    <title>请求代付流水表</title>
    <link rel="shortcut icon" href="${request.contextPath}/static/favicon.ico">
    <link href="${request.contextPath}/static/css/style_pay.css" rel="stylesheet" type="text/css"/>
    <script src="${request.contextPath}/static/js/jquery-1.11.1.min.js"></script>
    <script>
        $(function () {
            $("#navLi li").removeClass("active")
            $("#qqdf").addClass("active")
        });
    </script>
</head>
<body style="margin-top:50px;">
<#--页头-->
<#include "../../commons/header.ftl">
<form style="position: absolute; top: 55px;"  action="${request.contextPath}/reqDaifuList/save" method="post">
    <input type="hidden" name="id" value="<#if reqDaifuList.id??>${reqDaifuList.id}</#if>"/>
    <table class="gridtable" style="width:95%;">
        <tr>
            <th  style="text-align: center;" colspan="4">请求代付流水表信息 - [<a href="${request.contextPath}/reqDaifuList">返回</a>]</th>
        </tr>
        <tr>
            <th style="width: 120px;">订单号：</th>
            <td>
                <input  type="text" name="orderId" value="<#if reqDaifuList.orderId??>${reqDaifuList.orderId}</#if>"/>
            </td>
            <#if ShowApiAmount??>
            <th style="width: 120px;">金额(元)：</th>
            <td>
                <input type="text" name="amount" value="<#if reqDaifuList.amount??>${((reqDaifuList.amount?number)/100)?string(',##0.00')}</#if>"/>
            </td>
            </#if>

        </tr>
        <tr>
            <th>支付通道：</th>
            <td>
                <input  type="text" name="channel" value="<#if reqDaifuList.channel??>${reqDaifuList.channel}</#if>"/>
            </td>

            <#if ShowMemberID??>
            <th>商户号：</th>
            <td>
                <input  type="text" name="channelMemberId" value="<#if reqDaifuList.channelMemberId??>${reqDaifuList.channelMemberId}</#if>"/>
            </td>
            </#if>
        </tr>
        <tr>
            <th>时间：</th>
            <td>
                <input  type="text" name="timeStmp" value="<#if reqDaifuList.timeStmp??>${reqDaifuList.timeStmp?string('yyyy-MM-dd HH:mm:ss')}</#if>"/>
            </td>
            <th>处理结果：</th>
            <td>
                <input  type="text" name="result" value="<#if reqDaifuList.result??>${reqDaifuList.result}</#if>"/>
            </td>
        </tr>
        <tr>
            <th>请求代付信息：</th>
            <td colspan="3">
                <#if reqDaifuList.reqDaifuInfo??>
                    <table style="table-layout: fixed; width: 100%; ">
                       <tr>
                            <th style="width: 285px;text-align: left;">内存地址：</th>
                            <th style="text-align: left;"> ${reqDaifuList.reqDaifuInfo} </th>
                       </tr>


                        <#if showApiKey??>
                        <tr>
                            <th style="text-align: left;background: lightsteelblue;">私钥：</th>
                            <td style="word-wrap:break-word;">  ${reqDaifuList.reqDaifuInfo.API_KEY!} </td>
                        </tr>
                        </#if>


                        <tr>
                            <th style="text-align: left;background: lightsteelblue;">公钥：</th>
                            <td style="word-wrap:break-word;">
                            ${reqDaifuList.reqDaifuInfo.API_PUBLIC_KEY!}
                            </td>
                        </tr>


                        <tr>
                            <th style="text-align: left;">来自DB的其他参数：</th>
                            <td style="word-wrap:break-word;">
                            ${reqDaifuList.reqDaifuInfo.API_OTHER_PARAM!}
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">客户IP：</th>
                            <td style="word-wrap:break-word;">
                                <#if reqDaifuList?? && reqDaifuList.reqDaifuInfo?? && reqDaifuList.reqDaifuInfo.API_Client_IP??>${reqDaifuList.reqDaifuInfo.API_Client_IP!} -  ${IpHelperCZ.findStrAddress(reqDaifuList.reqDaifuInfo.API_Client_IP!)!}<#else> ...  </#if>
                            </td>
                        </tr>

                            <#if ShowIdAndOid??>
                            <tr>
                                <th style="text-align: left;">业主OID：</th>
                                <td style="word-wrap:break-word;">
                                    ${reqDaifuList.reqDaifuInfo.API_OID!}
                                </td>
                            </tr>
                            </#if>

                       <#if ShowMemberID??>
                        <tr>
                            <th style="text-align: left;background: lightsteelblue;">商户号：</th>
                            <td style="word-wrap:break-word;">
                               ${reqDaifuList.reqDaifuInfo.API_MEMBERID!}
                            </td>
                        </tr>
                        </#if>
                        <#if ShowApiAmount??>
                        <tr>
                            <th style="text-align: left;background: lightsteelblue;">金额：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_AMOUNT!}
                            </td>
                        </tr>
                        </#if>
                        <tr>
                            <th style="text-align: left;background: lightsteelblue;">订单号：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_ORDER_ID!}
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">订单时间：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_OrDER_TIME!}   [解析( <#if reqDaifuList.reqDaifuInfo.API_OrDER_TIME??> ${reqDaifuList.reqDaifuInfo.API_OrDER_TIME?number?number_to_datetime}<#else> ...  </#if>)]
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;background: lightsteelblue;">通道名称：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_CHANNEL_BANK_NAME!}
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;">订单状态：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_ORDER_STATE!}
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">回调域名：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_NOTIFY_URL_PREFIX!}
                            </td>
                        </tr>


                        <tr>
                            <th style="text-align: left;background: lightsalmon;">用户账号：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_CUSTOMER_ACCOUNT!}
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;background: lightsalmon;">用户姓名：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_CUSTOMER_NAME!}
                            </td>
                        </tr>


                        <tr>
                            <th style="text-align: left;background: lightsalmon;">银行名称：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_CUSTOMER_BANK_NAME!}
                            </td>
                        </tr>


                        <tr>
                            <th style="text-align: left;background: lightsalmon;">分行名称：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_CUSTOMER_BANK_BRANCH!}
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;background: lightsalmon;">分行所在省份：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_CUSTOMER_BANK_SUB_BRANCH!}
                            </td>
                        </tr>


                        <tr>
                            <th style="text-align: left;background: lightsalmon;">支行所在城市：</th>
                            <td>
                                ${reqDaifuList.reqDaifuInfo.API_CUSTOMER_BANK_NUMBER!}
                            </td>
                        </tr>


                    </table>
                </#if>

            </td>
        </tr>
        <tr>
            <th>请求代付结果：</th>
            <td colspan="3">
            <#if reqDaifuList.requestDaifuResult??>
                <table style="table-layout: fixed; width: 100%; ">

                    <tr>
                        <th style="width: 230px;text-align: left;">内存地址：</th>
                        <th style="text-align: left;"> ${reqDaifuList.requestDaifuResult} </th>
                    </tr>


                    <tr>
                        <th style="text-align: left;background: lightsteelblue;">总处理结果：</th>
                        <td style="word-wrap:break-word;">
                            ${reqDaifuList.requestDaifuResult.requestDaifuCode!}
                        </td>
                    </tr>


                    <tr>
                        <th style="text-align: left;">错误消息：</th>
                        <td style="word-wrap:break-word;">
                            ${reqDaifuList.requestDaifuResult.requestDaifuErrorMsg!?html?js_string}
                        </td>
                    </tr>
                   <#if ShowApiAmount??>
                    <tr>
                        <th style="text-align: left;background: lightsteelblue;">金额：</th>
                        <td style="word-wrap:break-word;">
                           ${reqDaifuList.requestDaifuResult.requestDaifuAmount!}
                        </td>
                    </tr>
                    </#if>
                    <tr>
                        <th style="text-align: left;background: lightsteelblue;">订单号：</th>
                        <td style="word-wrap:break-word;">
                           ${reqDaifuList.requestDaifuResult.requestDaifuOrderId!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;background: lightsteelblue;">订单状态：</th>
                        <td style="word-wrap:break-word;">
                            ${reqDaifuList.requestDaifuResult.requestDaifuOrderState!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;background: lightsteelblue;">OID：</th>
                        <td style="word-wrap:break-word;">
                            ${reqDaifuList.requestDaifuResult.requestDaifuOid!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;background: lightsteelblue;">支付通道：</th>
                        <td style="word-wrap:break-word;">
                            ${reqDaifuList.requestDaifuResult.requestDaifuChannelBankName!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">创建订单：</th>
                        <td style="word-wrap:break-word;">
                            ${reqDaifuList.requestDaifuResult.requestDaifuOrderCreateTime!}
                        </td>
                    </tr>


                    <tr>
                        <th style="text-align: left;">通道耗时：</th>
                        <td style="word-wrap:break-word;">
                        ${reqDaifuList.requestDaifuResult.requestDaifuChannelTime!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">DB接口耗时：</th>
                        <td style="word-wrap:break-word;">
                        ${reqDaifuList.requestDaifuResult.requestDaifuGetReqDaifuInfoTime!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">处理耗时：</th>
                        <td style="word-wrap:break-word;">
                            ${reqDaifuList.requestDaifuResult.requestDaifuTotalTime!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">过程详情:</th>
                        <td>
                             <#if reqDaifuList.requestDaifuResult.details??>
                                    <table style="table-layout: fixed; width: 100%; ">
                                       <#list reqDaifuList.requestDaifuResult.details?keys as key>
                                            <tr>
                                                <td style=" width: 10%;text-align: left;"> ${key} </td>
                                                <td style="word-wrap:break-word;"> ${reqDaifuList.requestDaifuResult.details[key]?html} </td>
                                            </tr>
                                       </#list>
                                </table>
                            </#if>
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">参数详情:</th>
                        <td>
                             <#if reqDaifuList.requestDaifuResult.params??>
                                 <table style="table-layout: fixed; width: 100%; ">
                                       <#list reqDaifuList.requestDaifuResult.params?keys as key>
                                           <tr>
                                               <td style=" width: 10%;text-align: left;"> ${key} </td>
                                               <td style="word-wrap:break-word;"> ${reqDaifuList.requestDaifuResult.params[key]?html} </td>
                                           </tr>
                                       </#list>
                                 </table>
                             </#if>
                        </td>
                    </tr>



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
