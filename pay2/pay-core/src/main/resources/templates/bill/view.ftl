<html>
<head>
    <title>支付订单信息</title>
    <link rel="shortcut icon" href="${request.contextPath}/static/favicon.ico">
    <link href="${request.contextPath}/static/css/style_pay.css" rel="stylesheet" type="text/css"/>
    <script src="${request.contextPath}/static/js/jquery-1.11.1.min.js"></script>
    <style type="text/css">
        th{width: 212px;text-align: left;}
        textarea{width: 100%; height: 65px;}
        .hiLight{background: lightslategrey;}
    </style>
    <script>
        $(function () {
            $("#navLi li").removeClass("active")
            $("#tdts").addClass("active")
        });
    </script>

</head>
<body style="margin-top:50px;"> <!-- overflow: hidden; -->
<#--页头-->
<#include "../commons/header.ftl">
<form  style="position: absolute; top: 55px;" action="${request.contextPath}/bills/save" method="post">
    <input type="hidden" name="id" value="<#if bill.id??>${bill.id!?c}</#if>"/>

    <table class="gridtable" style="width:90%;">
        <tr>
            <th style="text-align: center;">支付订单信息 - [<a href="${request.contextPath}/bills">返回</a>]</th>
        </tr>
        <#if msg??>
            <tr style="text-align: center;color:#00ba00;">
                <th>${msg}</th>
            </tr>
        </#if>
    </table>

    <table class="gridtable" style="table-layout:fixed;width:90%;">

        <tr>
            <th style="background: lightsteelblue;width: 340px;">API_KEY：私钥(密钥)</th>
            <td>
                <textarea name="API_KEY" ><#if bill.API_KEY??>${bill.API_KEY}</#if></textarea>
            </td>
        </tr>
        <tr>
            <th>API_PUBLIC_KEY：公钥</th>
            <td>
                <textarea name="API_PUBLIC_KEY" ><#if bill.API_PUBLIC_KEY??>${bill.API_PUBLIC_KEY}</#if></textarea>
            </td>
        </tr>
        <tr>
            <th>API_JUMP_URL_PREFIX：跳转URL</th>
            <td>
                <#--
                <input type="text" name="API_JUMP_URL_PREFIX" value="<#if bill.API_JUMP_URL_PREFIX??>${bill.API_JUMP_URL_PREFIX}<#else>http://66p.nsqmz6812.com:30000</#if>"/>
                -->
                <input type="text" name="API_JUMP_URL_PREFIX" value="<#if bill.API_JUMP_URL_PREFIX??>${bill.API_JUMP_URL_PREFIX}<#else>http://66p.badej8888.com:30000</#if>"/>
            </td>
        </tr>
        <tr>
            <th>API_WEB_URL：网页地址</th>
            <td>
                <input type="text" name="API_WEB_URL" value="<#if bill.API_WEB_URL??>${bill.API_WEB_URL}<#else>http://lot.huiek888.com/home</#if>"/>
            </td>
        </tr>

        <tr>
            <th>API_OTHER_PARAM：其他参数</th>
            <td>
                <input type="text" name="API_OTHER_PARAM" value="<#if bill.API_OTHER_PARAM??>${bill.API_OTHER_PARAM}<#else>this is other param from db reqpayInfo</#if>"/>
            </td>
        </tr>

        <tr>
            <th>API_Client_IP：客户IP</th>
            <td>
                <input type="text" name="API_Client_IP" value="<#if bill.API_Client_IP??>${bill.API_Client_IP}<#else>123.123.123.123</#if>"/>
            </td>
        </tr>

        <tr>
            <th>API_ORDER_FROM:订单来源<br>（6-web,9wap）</th>
            <td>
                <input type="text" name="API_ORDER_FROM" value="<#if bill.API_ORDER_FROM??>${bill.API_ORDER_FROM}<#else>WEB</#if>"/>
            </td>
        </tr>

        <tr>
            <th>API_OID：业主ID</th>
            <td>
                <input type="text" name="API_OID" value="<#if bill.API_OID??>${bill.API_OID}<#else>100</#if>"/>
            </td>
        </tr>

        <tr>
            <th>API_MEMBER_PLATFORMID：平台号</th>
            <td>
                <input type="text" name="API_MEMBER_PLATFORMID" value="<#if bill.API_MEMBER_PLATFORMID??>${bill.API_MEMBER_PLATFORMID}</#if>"/>
            </td>
        </tr>

        <tr>
            <th style="background: lightsteelblue;">API_MEMBERID：商户号</th>
            <td>
                <input type="text" name="API_MEMBERID" value="<#if bill.API_MEMBERID??>${bill.API_MEMBERID}</#if>"/>
            </td>
        </tr>


        <tr>
            <th style="background: lightsteelblue;">API_ORDER_ID：订单号</th>
            <td>
                <input type="text" name="API_ORDER_ID" value="<#if bill.API_ORDER_ID??>${bill.API_ORDER_ID}<#else>tony_${statics["java.lang.System"].currentTimeMillis()?c}</#if>"/>
            </td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;">API_CHANNEL_BANK_NAME：通道名</th>
            <td>
                <input type="text" name="API_CHANNEL_BANK_NAME" value="<#if bill.API_CHANNEL_BANK_NAME??>${bill.API_CHANNEL_BANK_NAME}</#if>"/>
            </td>
        </tr>
        <tr>
            <th style="background: lightsteelblue;">API_AMOUNT：订单金额(分)</th>
            <td>
                <input type="text" name="API_AMOUNT" value="<#if bill.API_AMOUNT??>${bill.API_AMOUNT?c}<#else>30000</#if>"/>  <#--${((bill.API_AMOUNT?number)/100)?string(',##0.00')}} -->
            </td>
        </tr>
        <tr>
            <th >API_TIME_OUT：超时(毫秒)</th>
            <td>
                <input type="text" name="API_TIME_OUT" value="<#if bill.API_TIME_OUT??>${bill.API_TIME_OUT?c}<#else>0</#if>"/>
            </td>

        </tr>
        <tr>
            <th>API_ORDER_STATE：订单状态<br>（0待确认，1已存入，2已取消,3已锁定）</th>
            <td>
                <input type="text" name="API_ORDER_STATE" value="<#if bill.API_ORDER_STATE??>${bill.API_ORDER_STATE}<#else>0</#if>"/>
            </td>

        </tr>
        <tr>
            <th>API_NOTIFY_URL_PREFIX：回调地址</th>
            <td>
                            <#--
                <input type="text" name="API_NOTIFY_URL_PREFIX" value="<#if bill.API_NOTIFY_URL_PREFIX??>${bill.API_NOTIFY_URL_PREFIX}<#else>http://66p.nsqmz6812.com:30000</#if>"/>
                -->
                <input type="text" name="API_NOTIFY_URL_PREFIX" value="<#if bill.API_NOTIFY_URL_PREFIX??>${bill.API_NOTIFY_URL_PREFIX}<#else>http://66p.badej8888.com:30000</#if>"/>
            </td>
        </tr>
        <tr>
            <th>API_ORDER_TIME：订单创建时间</th>
            <td>
                <input type="text" name="API_ORDER_TIME" value="<#if bill.API_ORDER_TIME??>${bill.API_ORDER_TIME?string('yyyy-MM-dd HH:mm:ss')}<#else>${.now?string("yyyy-MM-dd HH:mm:ss")}</#if>"/>
            </td>
        </tr>
        <tr>
            <th style="background: lightsalmon;">API_CUSTOMER_ACCOUNT：会员账号</th>
            <td>
                <input type="text" name="API_CUSTOMER_ACCOUNT" value="<#if bill.API_CUSTOMER_ACCOUNT??>${bill.API_CUSTOMER_ACCOUNT}<#else>wangxiaojun</#if>"/>
            </td>
        </tr>
        <tr>
            <th  style="background: lightsalmon;">API_CUSTOMER_NAME：会员姓名</th>
            <td>
                <input type="text" name="API_CUSTOMER_NAME" value="<#if bill.API_CUSTOMER_NAME??>${bill.API_CUSTOMER_NAME}<#else>王小军</#if>"/>
            </td>
        </tr>
        <tr>
            <th  style="background: lightsalmon;">API_CUSTOMER_BANK_NAME：银行名称</th>
            <td>
                <input type="text" name="API_CUSTOMER_BANK_NAME" value="<#if bill.API_CUSTOMER_BANK_NAME??>${bill.API_CUSTOMER_BANK_NAME}<#else>中国建设银行</#if>"/>
            </td>
        </tr>
        <tr>
            <th  style="background: lightsalmon;" >API_CUSTOMER_BANK_BRANCH：分行所在省份</th>
            <td>
                <input type="text" name="API_CUSTOMER_BANK_BRANCH" value="<#if bill.API_CUSTOMER_BANK_BRANCH??>${bill.API_CUSTOMER_BANK_BRANCH}<#else>陕西省分行</#if>"/>
            </td>
        </tr>
        <tr>
            <th  style="background: lightsalmon;">API_CUSTOMER_BANK_SUB_BRANCH：支行所在城市</th>
            <td>
                <input type="text" name="API_CUSTOMER_BANK_SUB_BRANCH" value="<#if bill.API_CUSTOMER_BANK_SUB_BRANCH??>${bill.API_CUSTOMER_BANK_SUB_BRANCH}<#else>永寿县支行</#if>"/>
            </td>
        </tr>
        <tr>
            <th  style="background: lightsalmon;">API_CUSTOMER_BANK_NUMBER：银行账号</th>
            <td>
                <input type="text" name="API_CUSTOMER_BANK_NUMBER" value="<#if bill.API_CUSTOMER_BANK_NUMBER??>${bill.API_CUSTOMER_BANK_NUMBER}<#else>6217004160022335741</#if>"/>
            </td>
        </tr>

        <tr>
            <td colspan="2"><input style="width: 5%;" type="submit" value="保存"/></td>
        </tr>

    </table>
</form>
</body>
</html>
