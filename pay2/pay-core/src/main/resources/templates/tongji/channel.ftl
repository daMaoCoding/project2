<!DOCTYPE html>
<html>
<head>
    <title>统计-支付通道</title>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <meta name="renderer" content="webkit">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="apple-mobile-web-app-status-bar-style" content="black">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="format-detection" content="telephone=no">
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

    <!--日期选择框 -->
    <script src="${request.contextPath}/static/layDate-v5.0.7/laydate.js"></script>

     <!--Echarts -->
    <script src="${request.contextPath}/static/js/echarts.common.min.js"></script>


    <!-- 引入Vue + iVue -->
    <script src="${request.contextPath}/static/vue/vue-v2.5.1.min.js"></script>
    <link rel="stylesheet" href="${request.contextPath}/static/vue/iview-2.5.1.css">
    <script src="${request.contextPath}/static/vue/iview-2.5.1.min.js"></script>



    <style type="text/css">
        td {white-space:nowrap;overflow:hidden;word-break:keep-all;text-overflow:ellipsis}
        .betNotice .betClose {  height: 25px;  color: #fff;  text-align: center;  clear: both; }
        .betNotice {  line-height: 18px;  color: #666;  white-space: nowrap;  }
        .betNotice .betClose span {  cursor: pointer; display: inline-block;  border-bottom-left-radius: 5px; border-bottom-right-radius: 5px; background:  rgb(55, 164, 219); }
        .betNotice .betClose { color: #fff; text-align: center; }
        .betNotice .betClose i { font-size: 13px;  padding: 0 15px;  }

    </style>
    <script>
        var t = function t() {
             if($("#chevron").hasClass("ivu-icon-chevron-up")){
                 $("#chevron").removeClass("ivu-icon-chevron-up").addClass("ivu-icon-chevron-down");
                 $("#searchForm").hide();
             }else{
                 $("#chevron").removeClass("ivu-icon-chevron-down").addClass("ivu-icon-chevron-up")
                 $("#searchForm").show();
             }
        }


        $(function () {
            $("#navLi li").removeClass("active")
            $("#zftdtj").addClass("active")
        });
        $(function () { $("#qqcgl").tooltip({html : true });});
        $(function () { $("#zzcgl").tooltip({html : true });});

        $(function () {

            //日期时间范围
            laydate.render({
                elem: '#riQiFanWei',
                type: 'datetime',
               // value: '2017-01-01 00:00:00 - 2017-12-31 23:59:59',
                range: true
            });

                  // 基于准备好的dom，初始化echarts实例
                  var echartsMain = echarts.init(document.getElementById('echartsMain'));


                  //请求图形数据
                  openEchar = function openEchar(url,channelName,channelCName){
                      echartsMain.showLoading();//图形加载中
                      $("#echartsModalTitle").html(channelName+" : "+channelCName); //设置标题


                      $.post(url).done(function(data){ //请求数据
                          echartsMain.hideLoading();
                         // echartsMain.setOption(option);   // 使用刚指定的配置项和数据显示图表。


                          //console.log(data)

                          echartsMain.setOption({
                              title: {
                                  text: '成功率统计'
                              },
                              tooltip : {
                                  trigger: 'axis',
                                  axisPointer: {
                                      type: 'cross',
                                      label: {
                                          backgroundColor: '#6a7985'
                                      }
                                  }
                              },
                              legend: {
                                  data:['请求总数','请求成功','支付成功',"支付金额"]
                              },
                              toolbox: {
                                  feature: {
                                      saveAsImage: {}
                                  }
                              },
                              grid: {
                                  left: '0%',
                                  right: '0%',
                                  bottom: '1%',
                                  containLabel: true
                              },
                              xAxis :
                                      {
                                          boundaryGap: false,
                                          data: data.map(function (item) {
                                              return item[0];
                                          })
                                          //type : 'category',
                                          // boundaryGap : false,
                                          //data : ['周一','周二','周三','周四','周五','周六','周日']
                                      },
                              yAxis :
                                      {
                                          //type : 'value'
                                          splitLine: {
                                              show: false
                                          }
                                      },

                              dataZoom: [{
                                  //startValue: '2000-06-02'
                              }, {
                                  type: 'inside'
                              }],


                              series : [
                                  {
                                      name:'请求总数',
                                      type:'line',
                                      smooth: true,
                                      symbol: 'circle',
                                      symbolSize: 5,
                                      sampling: 'average',
                                      itemStyle: {
                                          normal: {
                                              color: '#8ec6ad'
                                          }
                                      },
                                      areaStyle: {
                                          normal: {
                                              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                                                  offset: 0,
                                                  color: '#8ec6ad'
                                              }, {
                                                  offset: 1,
                                                  color: '#ffe'
                                              }])
                                          }
                                      },
                                      stack: '总量',
                                      // areaStyle: {normal: {}},
                                      // data:[100, 200, 300, 400, 500, 600, 700]
                                      data: data.map(function (item) {
                                          return item[1];
                                      }),

                                  },
                                  {
                                      name:'请求成功',
                                      type:'line',
                                      smooth: true,
                                      symbol: 'circle',
                                      symbolSize: 5,
                                      sampling: 'average',
                                      itemStyle: {
                                          normal: {
                                              color: '#7e0023'
                                          }
                                      },
                                      areaStyle: {
                                          normal: {
                                              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                                                  offset: 0,
                                                  color: '#7e0023'
                                              }, {
                                                  offset: 1,
                                                  color: '#ffe'
                                              }])
                                          }
                                      },
                                      stack: '总量',
                                      //areaStyle: {normal: {}},
                                      //data:[99, 199, 299, 399, 499, 599, 699]
                                      data: data.map(function (item) {
                                          return item[2];
                                      }),
                                  },
                                  {
                                      name:'支付成功',
                                      type:'line',
                                      smooth: true,
                                      symbol: 'circle',
                                      symbolSize: 5,
                                      sampling: 'average',
                                      itemStyle: {
                                          normal: {
                                              color: '#ffde33'
                                          }
                                      },
                                      areaStyle: {
                                          normal: {
                                              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                                                  offset: 0,
                                                  color: '#ffde33'
                                              }, {
                                                  offset: 1,
                                                  color: '#ffe'
                                              }])
                                          }
                                      },
                                      stack: '总量',
                                      //areaStyle: {normal: {}},
                                      // data:[90, 190, 290, 390, 490, 590, 690]
                                      data: data.map(function (item) {
                                          return item[3];
                                      }),
                                  },
                                  {
                                      name:'支付金额',
                                      type:'line',
                                      smooth: true,
                                      symbol: 'circle',
                                      symbolSize: 5,
                                      sampling: 'average',
                                      itemStyle: {
                                          normal: {
                                              color: '#660099'
                                          }
                                      },
                                      areaStyle: {
                                          normal: {
                                              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                                                  offset: 0,
                                                  color: '#660099'
                                              }, {
                                                  offset: 1,
                                                  color: '#ffe'
                                              }])
                                          }
                                      },
                                      stack: '总量',
                                      // areaStyle: {normal: {}},
                                      // data:[500, 600, 700, 800, 900, 1000, 1100]
                                      data: data.map(function (item) {
                                          return item[4];
                                      }),
                                  }
                              ]
                          });
                    });

               $('#echartsModal').modal({ //展现模态框
                    keyboard: true
                })

            }


        });
    </script>

</head>
<body>
<#--页头-->
<#include "../commons/header.ftl">
<div class="container-fluid">
    <div>
        <h1 class="hidden text-center"><a>统计.[通道成功率]</a></h1>

     <div class="betNotice">
         <div id="searchForm" class="">
        <form  class="form-inline"  action="${request.contextPath}/tongji/index/" method="post">
            <table class="table-bordered table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
                <tr>
                    <th class="text-right">日期范围：</th>
                    <td>
                        <input  style="width: 100%;" class="form-control input-sm"  type="text" name="riQiFanWei" id="riQiFanWei" value="<#if queryParam.riQiFanWei??>${queryParam.riQiFanWei}</#if>"/>
                    </td>
                    <th class="text-right">支付通道</th>
                    <td>
                        <input  style="width: 100%;" class="form-control input-sm"  type="text" name="channelName" value="<#if queryParam.channelName??>${queryParam.channelName}</#if>"/>
                    </td>
                    <th class="text-right">第三方ID：</th>
                    <td>
                       <#-- <input style="width: 100%;" class="form-control input-sm" type="text" name="channelPrefix"  value="<#if queryParam.channelPrefix??>${queryParam.channelPrefix}</#if>"/>-->
                        <select id="channelPrefix" name="channelPrefix" class="selectpicker show-menu-arrow form-control"  style="width: 100%;" >
                            <#if payCoMaps?exists>
                                <option  <#if  !queryParam.channelPrefix?? || queryParam.channelPrefix=="ALL" >SELECTED</#if>   value="ALL">全部</option>
                                 <#list payCoMaps?keys as key>
                                  <option <#if  queryParam.channelPrefix?? && queryParam.channelPrefix==key>SELECTED </#if>  value="${key}">${payCoMaps[key]}</option>
                                 </#list>
                            </#if>
                        </select>



                    </td>
                    <td rowspan="2" style="width: 9%;">
                        <button type="submit" class="btn btn-success center-block" ><span class="ivu-icon ivu-icon-checkmark"></span> 查询</button>
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

                    <th class="text-right">统计类型：</th>
                    <td>
                        <select id="tongJiType" name="tongJiType" class="selectpicker show-menu-arrow form-control"  style="width: 100%;" >
                            <#if payTypeMaps?exists>
                                <option  <#if  !queryParam.tongJiType?? || queryParam.tongJiType=="ALL" >SELECTED</#if>   value="ALL">全部</option>
                                 <#list payTypeMaps?keys as key>
                                  <option <#if  queryParam.tongJiType?? && queryParam.tongJiType==key>SELECTED </#if>  value="${key}">${payTypeMaps[key]}</option>
                                 </#list>
                            </#if>
                        </select>
                    </td>
                </tr>
            </table>
        </form>
     </div>


    <#if pageInfo??>
        <div class="hidden text-center">
            <a role="button"  data-toggle="collapse" href="#collapsePageInfo" aria-expanded="false" aria-controls="collapseExample" >=-分页信息-= </a>
        </div>



        <div class="panel-group" style="margin-bottom: 0px;" id="accordion">
            <div class="panel panel-info">
                <div class="panel-heading" >
                    <h4 class="panel-title">
                          <a   style="float: right;margin-right: 45px" onclick="openEchar('${request.contextPath}/tongji/cgl/allChannel/${queryParam.tongJiType}/${queryParam.riQiFanWei}/${queryParam.oid}/','AllChannel','全部通道')" ref="#">[图表]</a>
                        [汇总统计]：
                        请求成功率:<#if (allCglTotal.pageReqSum!?c?number)==0>0%  &nbsp;&nbsp; / &nbsp;&nbsp;<#else> ${allCglTotal.pageReqSuccessSum!}/${allCglTotal.pageReqSum!} = ${(((allCglTotal.pageReqSuccessSum!?c?number)/(allCglTotal.pageReqSum!?c?number))*100)?string(',##0.00') }%  &nbsp;&nbsp; / &nbsp;&nbsp;</#if>
                        支付成功率:<#if (allCglTotal.pageReqSuccessSum!?c?number)==0>0%  &nbsp;&nbsp; / &nbsp;&nbsp;<#else> ${allCglTotal.pageResSuccessSum!}/${allCglTotal.pageReqSuccessSum!}= ${(((allCglTotal.pageResSuccessSum!?c?number)/(allCglTotal.pageReqSuccessSum!?c?number))*100)?string(',##0.00') }  %  &nbsp;&nbsp; / &nbsp;&nbsp;</#if>
                        成功入款: <#if (allCglTotal.pageReqSum!?c?number)==0>0.00元 &nbsp;&nbsp; / &nbsp;&nbsp;<#else>  ${((((allCglTotal.pageResSuccessAmount!0)?c)?number)/100)?string(',##0.00')} 元 &nbsp;&nbsp; / &nbsp;&nbsp;</#if>
                        共${pageInfo.total}条记录   &nbsp;&nbsp; / &nbsp;&nbsp; 报警(请求成功率低于:${fmk.reqWarning!}%,&nbsp;支付成功率低于:${fmk.resWarning!}%)
                   </h4>
                </div>

            </div>
        </div>


        <div class="betClose"><span onclick="t()"><span><i id="chevron" class="ivu-icon ivu-icon-chevron-up"></i></span> <span style="display: none;"><i class="ivu-icon ivu-icon-chevron-down"></i></span></span></div>

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



        <h3 class="hidden text-success bg-info text-center" style="margin-bottom: 0px;">-= 查询结果 =-</h3>

        <table class="table table-bordered table-hover table-striped table-condensed" style="table-layout: fixed;width:100%;text-align: center;">
            <thead>
            <tr style="background-color: #337ab7;color:white;"  class="bg-info">
                <th  style="width: 2%;" class="text-center" >序</th>
                <th  style="width: 11%;" class="text-center" >通道名称</th>
                <th  style="width: 8%;" class="text-center" >中文名称</th>
                <th  style="width: 5%;" class="text-center" >请求总数</th>
                <th  style="width: 5%;" class="text-center" >请求成功数</th>
                <th  style="width: 5%;" class="text-center tooltip-options" >
                    请求成功率 <a  style="color: rgb(255, 255, 255);" id="qqcgl" class="glyphicon glyphicon-question-sign"  href="#"  data-toggle="tooltip"  title="<h4>请求成功÷请求总数%</h4>"></a>
                </th>
                <th  style="width: 4%;" class="text-center" >请求成功额</th>
                <th  style="width: 5%;" class="text-center" >响应成功数</th>
                <th  style="width: 5%;" class="text-center  tooltip-options" >
                    支付成功率 <a style="color: rgb(255, 255, 255);"  id="zzcgl" class="glyphicon glyphicon-question-sign"  href="#"  data-toggle="tooltip"  title="<h4>响应成功÷请求成功%</h4>"></a>
                </th>
                <th  style="width: 4%;" class="text-center" >成功入款</th>
                <th  style="width: 5%;" class="text-center">错误 / 图表</th>
            </tr>
            </thead>
            <tbody>
                <#list pageInfo.list as tongJi>
                <#if tongJi.resSuccessDivReqSuccess?number<fmk.resWarning!  || tongJi.resSuccessDivReqSuccess?number gt 100  ><tr class="danger"><#else> <tr class="success"></#if>
               <#-- <tr>-->
                    <td>${tongJi_index!}</td>
                    <td class="text-left">${tongJi.pageChannelName!}</td>
                    <td class="text-left">${tongJi.channelCName!}</td>
                    <td style="text-overflow: ellipsis">${tongJi.pageReqSum!}</td>
                    <td><#if tongJi.pageReqSuccessSum !="0"> ${tongJi.pageReqSuccessSum}<#else> </#if></td>
                    <#if tongJi.reqSuccessDivReqSum?number<fmk.reqWarning!  || tongJi.reqSuccessDivReqSum?number gt 100  ><td class="danger"><#else> <td class="success"></#if>
                         <#if tongJi.reqSuccessDivReqSum !="0.00">${tongJi.reqSuccessDivReqSum} %</#if>
                    </td>
                    <td class="text-right"><#if tongJi.pageReqSuccessAmount !="0"> ${((tongJi.pageReqSuccessAmount?number)/100)?string(',##0.00')} 元<#else> </#if></td>
                    <td><#if tongJi.pageResSuccessSum !="0"> ${tongJi.pageResSuccessSum}<#else> </#if></td>
                    <td><#if tongJi.resSuccessDivReqSuccess !="0.00">${tongJi.resSuccessDivReqSuccess} %</#if></td>
                    <td class="text-right"><#if tongJi.pageResSuccessAmount !="0"> ${((tongJi.pageResSuccessAmount?number)/100)?string(',##0.00')} 元<#else> </#if></td>
                    <td style="text-align:center;">
                        [<a target="view_window" href='${request.contextPath}/reqPayList?channel=${tongJi.pageChannelName}&result=ERROR' >错误</a>] /
                        [<a onclick="openEchar('${request.contextPath}/tongji/cgl/${tongJi.pageChannelName}/${queryParam.riQiFanWei}/${queryParam.oid}/','${tongJi.pageChannelName}','${tongJi.channelCName}')" ref="#">图表</a>]
                    </td>
                </td>
                </#list>
            </tbody>
        </table>





        <nav class="text-right" aria-label="Page navigation">
            <ul class="pagination">
                <#if pageInfo.hasPreviousPage>
                    <li>
                        <a href="${request.contextPath}/tongji/index/?page=1&rows=${pageInfo.pageSize!?c}&riQiFanWei=${queryParam.riQiFanWei}&channelName=${queryParam.channelName}&oid=${queryParam.oid}&tongJiType=${queryParam.tongJiType}&channelPrefix=${queryParam.channelPrefix}">首页</a>
                    </li>
                    <li>
                        <a href="${request.contextPath}/tongji/index/?page=${pageInfo.prePage}&rows=${pageInfo.pageSize!?c}&riQiFanWei=${queryParam.riQiFanWei}&channelName=${queryParam.channelName}&oid=${queryParam.oid}&tongJiType=${queryParam.tongJiType}&channelPrefix=${queryParam.channelPrefix}">前一页</a>
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
                            <a href="${request.contextPath}/tongji/index/?page=${nav!?c}&rows=${pageInfo.pageSize!?c}&riQiFanWei=${queryParam.riQiFanWei}&channelName=${queryParam.channelName}&oid=${queryParam.oid}&tongJiType=${queryParam.tongJiType}&channelPrefix=${queryParam.channelPrefix}">${nav!?c}</a>
                        </li>
                    </#if>
                </#list>
                <#if pageInfo.hasNextPage>
                    <li>
                        <a href="${request.contextPath}/tongji/index/?page=${pageInfo.nextPage!?c}&rows=${pageInfo.pageSize!?c}&riQiFanWei=${queryParam.riQiFanWei}&channelName=${queryParam.channelName}&oid=${queryParam.oid}&tongJiType=${queryParam.tongJiType}&channelPrefix=${queryParam.channelPrefix}">下一页</a>
                    </li>
                    <li>
                        <a href="${request.contextPath}/tongji/index/?page=${pageInfo.pages!?c}&rows=${pageInfo.pageSize!?c}&riQiFanWei=${queryParam.riQiFanWei}&channelName=${queryParam.channelName}&oid=${queryParam.oid}&tongJiType=${queryParam.tongJiType}&channelPrefix=${queryParam.channelPrefix}">尾页</a>
                    </li>
                </#if>
                </li>
            </ul>
        </nav>
    </#if>
    </div>

    <!-- 图表模态框 -->
    <div class="push">

        <!-- 模态框（Modal） -->
        <div class="modal fade" id="echartsModal" tabindex="-1" aria-hidden="true" role="dialog" aria-labelledby="echartsModalTitle" aria-hidden="true">
            <div class="modal-dialog" style="width:1000px">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="echartsModalTitle">   </h4>
                    </div>
                    <div class="modal-body">
                    <div id="echartsMain" style="width: 950px;height:700px; overflow:auto;"></div>
                </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary" data-dismiss="modal">关闭</button>
                    </div>
                </div><!-- /.modal-content -->
            </div><!-- /.modal -->
        </div>
    </div>


</div>
</body>
</html>