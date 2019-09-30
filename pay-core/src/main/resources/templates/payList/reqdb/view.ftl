<html>
<head>
    <title>请求支付流水表</title>
    <link rel="shortcut icon" href="${request.contextPath}/static/favicon.ico">
    <link href="${request.contextPath}/static/css/style_pay.css" rel="stylesheet" type="text/css"/>
    <script src="${request.contextPath}/static/js/jquery-1.11.1.min.js"></script>
    <script>
        $(function () {
            $("#navLi li").removeClass("active")
            $("#qqzf").addClass("active")
        });
    </script>
</head>
<body style="margin-top:50px;">
<#--页头-->
<#include "../../commons/header.ftl">
<form style="position: absolute; top: 55px;"  action="${request.contextPath}/reqPayList/save" method="post">
    <input type="hidden" name="id" value="<#if reqPayList.id??>${reqPayList.id}</#if>"/>
    <table class="gridtable" style="width:95%;">
        <tr>
            <th  style="text-align: center;" colspan="4">请求支付流水表信息 - [<a href="${request.contextPath}/reqPayList">返回</a>]</th>
        </tr>
        <tr>
            <th style="width: 120px;">订单号：</th>
            <td>
                <input  type="text" name="orderId" value="<#if reqPayList.orderId??>${reqPayList.orderId}</#if>"/>
            </td>
            <#if ShowApiAmount??>
            <th style="width: 120px;">金额(元)：</th>
            <td>
                <input type="text" name="amount" value="<#if reqPayList.amount??>${((reqPayList.amount?number)/100)?string(',##0.00')}</#if>"/>
            </td>
            </#if>

        </tr>
        <tr>
            <th>支付通道：</th>
            <td>
                <input  type="text" name="channel" value="<#if reqPayList.channel??>${reqPayList.channel}</#if>"/>
            </td>

            <#if ShowMemberID??>
            <th>商户号：</th>
            <td>
                <input  type="text" name="channelMemberId" value="<#if reqPayList.channelMemberId??>${reqPayList.channelMemberId}</#if>"/>
            </td>
            </#if>
        </tr>
        <tr>
            <th>时间：</th>
            <td>
                <input  type="text" name="timeStmp" value="<#if reqPayList.timeStmp??>${reqPayList.timeStmp?string('yyyy-MM-dd HH:mm:ss')}</#if>"/>
            </td>
            <th>处理结果：</th>
            <td>
                <input  type="text" name="result" value="<#if reqPayList.result??>${reqPayList.result}</#if>"/>
            </td>
        </tr>
        <tr>
            <th>请求支付信息：</th>
            <td colspan="3">
                <#if reqPayList.reqPayInfo??>
                    <table style="table-layout: fixed; width: 100%; ">
                       <tr>
                            <th style="width: 285px;text-align: left;">内存地址：</th>
                            <th style="text-align: left;"> ${reqPayList.reqPayInfo} </th>
                       </tr>


                        <#if showApiKey??>
                        <tr>
                            <th style="text-align: left;">私钥：</th>
                            <td style="word-wrap:break-word;">  ${reqPayList.reqPayInfo.API_KEY!} </td>
                        </tr>
                        </#if>


                        <tr>
                            <th style="text-align: left;">公钥：</th>
                            <td style="word-wrap:break-word;">
                            ${reqPayList.reqPayInfo.API_PUBLIC_KEY!}
                            </td>
                        </tr>


                        <tr>
                            <th style="text-align: left;">跳转网址：</th>
                            <td style="word-wrap:break-word;">
                            ${reqPayList.reqPayInfo.API_JUMP_URL_PREFIX!}
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">购彩网址：</th>
                            <td style="word-wrap:break-word;">
                            ${reqPayList.reqPayInfo.API_WEB_URL!}
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">来自DB的其他参数：</th>
                            <td style="word-wrap:break-word;">
                            ${reqPayList.reqPayInfo.API_OTHER_PARAM!}
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">客户IP：</th>
                            <td style="word-wrap:break-word;">
                                <#if reqPayList?? && reqPayList.reqPayInfo?? && reqPayList.reqPayInfo.API_Client_IP??>${reqPayList.reqPayInfo.API_Client_IP!} -  ${IpHelperCZ.findStrAddress(reqPayList.reqPayInfo.API_Client_IP!)!}<#else> ...  </#if>
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">订单来源：</th>
                            <td style="word-wrap:break-word;">
                                ${reqPayList.reqPayInfo.API_ORDER_FROM!}  (备注：3 APP-Android，4 APP-IOS，5 APP-Other，6 WEB，7 Windows，8 Mac,9 WAP)
                            </td>
                        </tr>
                            <#if ShowIdAndOid??>
                            <tr>
                                <th style="text-align: left;">业主OID：</th>
                                <td style="word-wrap:break-word;">
                                    ${reqPayList.reqPayInfo.API_OID!}
                                </td>
                            </tr>
                            </#if>

                       <#if ShowMemberID??>
                        <tr>
                            <th style="text-align: left;">商户号：</th>
                            <td style="word-wrap:break-word;">
                               ${reqPayList.reqPayInfo.API_MEMBERID!}
                            </td>
                        </tr>
                        </#if>
                        <#if ShowApiAmount??>
                        <tr>
                            <th style="text-align: left;">金额<font size=2>(单位分)</font>： </th>
                            <td>
                                ${reqPayList.reqPayInfo.API_AMOUNT!}
                            </td>
                        </tr>
                        </#if>
                        <tr>
                            <th style="text-align: left;">订单号：</th>
                            <td>
                                ${reqPayList.reqPayInfo.API_ORDER_ID!}
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">订单时间：</th>
                            <td>
                                ${reqPayList.reqPayInfo.API_OrDER_TIME!}   [解析( <#if reqPayList.reqPayInfo.API_OrDER_TIME??> ${reqPayList.reqPayInfo.API_OrDER_TIME?number?number_to_datetime}<#else> ...  </#if>)]
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;">通道名称：</th>
                            <td>
                                ${reqPayList.reqPayInfo.API_CHANNEL_BANK_NAME!}
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">超时时间：</th>
                            <td>
                                ${reqPayList.reqPayInfo.API_TIME_OUT!}
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">订单状态：</th>
                            <td>
                                ${reqPayList.reqPayInfo.API_ORDER_STATE!}
                            </td>
                        </tr>
                        <tr>
                            <th style="text-align: left;">回调域名：</th>
                            <td>
                                ${reqPayList.reqPayInfo.API_NOTIFY_URL_PREFIX!}
                            </td>
                        </tr>

                        <tr>
                            <th style="text-align: left;">访问次数(wap)：</th>
                            <td>
                                ${reqPayList.restView!}
                            </td>
                        </tr>


                    </table>
                </#if>

            </td>
        </tr>
        <tr>
            <th>请求支付结果：</th>
            <td colspan="3">
            <#if reqPayList.requestPayResult??>
                <table style="table-layout: fixed; width: 100%; ">

                    <tr>
                        <th style="width: 230px;text-align: left;">内存地址：</th>
                        <th style="text-align: left;"> ${reqPayList.requestPayResult} </th>
                    </tr>


                    <tr>
                        <th style="text-align: left;">总处理结果：</th>
                        <td style="word-wrap:break-word;">
                            ${reqPayList.requestPayResult.requestPayCode!}
                        </td>
                    </tr>
                    <tr>
                        <th style="text-align: left;">二维码网址：</th>
                        <td style="word-wrap:break-word;">
                            ${reqPayList.requestPayResult.requestPayQRcodeURL!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">二维码内容：</th>
                        <td style="word-wrap:break-word;">
                            ${reqPayList.requestPayResult.requestPayQRcodeContent!}
                        </td>
                    </tr>


                    <tr>
                        <th style="text-align: left;">HTML内容：</th>
                        <td style="word-wrap:break-word;">
                             ${reqPayList.requestPayResult.requestPayHtmlContent!?html}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">跳转地址：</th>
                        <td style="word-wrap:break-word;">
                            ${reqPayList.requestPayResult.requestPayJumpToUrl!?html}
                        </td>
                    </tr>


                    <tr>
                        <th style="text-align: left;">错误消息：</th>
                        <td style="word-wrap:break-word;">
                            ${reqPayList.requestPayResult.requestPayErrorMsg!?html?js_string}
                        </td>
                    </tr>
                   <#if ShowApiAmount??>
                    <tr>
                        <th style="text-align: left;">金额：</th>
                        <td style="word-wrap:break-word;">
                           ${reqPayList.requestPayResult.requestPayamount!}
                        </td>
                    </tr>
                    </#if>
                    <tr>
                        <th style="text-align: left;">订单号：</th>
                        <td style="word-wrap:break-word;">
                           ${reqPayList.requestPayResult.requestPayOrderId!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">创建订单：</th>
                        <td style="word-wrap:break-word;">
                            ${reqPayList.requestPayResult.requestPayOrderCreateTime!}
                        </td>
                    </tr>


                    <tr>
                        <th style="text-align: left;">支付通道：</th>
                        <td style="word-wrap:break-word;">
                           ${reqPayList.requestPayResult.requestPayChannelBankName!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">通道耗时：</th>
                        <td style="word-wrap:break-word;">
                        ${reqPayList.requestPayResult.requestPayChannelTime!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">DB接口耗时：</th>
                        <td style="word-wrap:break-word;">
                        ${reqPayList.requestPayResult.requestPayGetReqpayinfoTime!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">处理耗时：</th>
                        <td style="word-wrap:break-word;">
                            ${reqPayList.requestPayResult.requestPayTotalTime!}
                        </td>
                    </tr>

                    <tr>
                        <th style="text-align: left;">过程详情:</th>
                        <td>
                             <#if reqPayList.requestPayResult.detail??>
                                    <table style="table-layout: fixed; width: 100%; ">
                                    <#list reqPayList.requestPayResult.detail as item>


                                        <tr>
                                            <th style=" width: 190px;text-align: left;"> 索引:</th>
                                            <th style="word-wrap:break-word;"> ${item_index}</th>
                                        </tr>


                                        <#if item??>
                                            <#list item?keys as key>
                                                <tr>
                                                    <td style=" width: 16px;text-align: left;"> ${key} </td>
                                                    <td style="word-wrap:break-word;"> ${item[key]?html} </td>
                                                </tr>
                                            </#list>
                                        </#if>
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
