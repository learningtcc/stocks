<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>日K列表</title>
    <meta http-equiv="X-UA-Compatible" content="IE=EmulateIE8"/>
    <link rel="stylesheet" type="text/css" href="<%=contextPath%>/vendor/bootstrap-v3.0/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="<%=contextPath%>/style/standard/css/eccrm-common-new.css">
    <script type="text/javascript" src="<%=contextPath%>/static/ycrl/javascript/jquery-all.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/ycrl/javascript/angular-all.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/static/ycrl/javascript/angular-strap-all.js"></script>
    <script type="text/javascript" src="<%=contextPath%>/vendor/My97DatePicker/WdatePicker.js"></script>
    <script>
        window.angular.contextPathURL = '<%=contextPath%>';
    </script>
    <style>
        .red{
            color: #ff2409;
        }
        .green{
            color: #0f900a;
        }
    </style>
</head>
<body>
<div class="main condition-row-1" ng-app="stock.stock.stockDay.list" ng-controller="Ctrl">
    <div class="list-condition">
        <div class="block">
            <div class="block-header">
                    <span class="header-button">
                        <a type="button" class="btn btn-green btn-min" ng-click="reset();"> 重置 </a>
                        <a type="button" class="btn btn-green btn-min" ng-click="query();"> 查询 </a>
                    </span>
            </div>
            <div class="block-content">
                <div class="content-wrap">
                    <div class="row float">
                        <div class="item w200">
                            <div class="form-label w80">
                                <label>股票编号:</label>
                            </div>
                            <input type="text" class="w120" ng-model="condition.code"
                                   maxlength="10"/>
                        </div>
                        <div class="item w200">
                            <div class="form-label w80">
                                <label>K线组合:</label>
                            </div>
                            <input type="text" class="w120" ng-model="condition.key"
                                   maxlength="10"/>
                        </div>
                        <div class="item w200">
                            <div class="form-label w80">
                                <label>开始时间:</label>
                            </div>
                            <div class="pr w120">
                                <input type="text" class="w120" ng-model="condition.businessDateGe" readonly
                                       eccrm-my97="{}"/>
                                <span class="add-on">
                                    <i class="icons icon clock" ng-click="condition.businessDateGe=null"
                                       title="点击清除"></i>
                                </span>
                            </div>
                        </div>
                        <div class="item w200">
                            <div class="form-label w80">
                                <label>截止时间:</label>
                            </div>
                            <div class="pr w120">
                                <input type="text" class="w120" ng-model="condition.businessDateLt" readonly
                                       eccrm-my97="{}"/>
                                <span class="add-on">
                                    <i class="icons icon clock" ng-click="condition.businessDateLt=null"
                                       title="点击清除"></i>
                                </span>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="list-result ">
        <div class="block">
            <div class="block-header">
                <div class="header-text">
                    <span>日K列表</span>
                </div>
                <span class="header-button">
                        <a type="button" class="btn btn-green btn-min" ng-click="exportData();"
                           ng-disabled="!pager.total" ng-cloak> 导出数据 </a>
                </span>
            </div>
            <div class="block-content">
                <div class="content-wrap">
                    <div class="table-responsive panel panel-table">
                        <table class="table table-striped table-hover">
                            <thead class="table-header">
                            <tr>
                                <td class="width-min">序号</td>
                                <td>股票编号</td>
                                <td>6线组合</td>
                                <td>日期段</td>
                                <td>第七日_h</td>
                                <td>第七日_l</td>
                                <td>阴阳状态</td>
                                <td>第一日</td>
                                <td>第二日</td>
                                <td>第三日</td>
                                <td>第四日</td>
                                <td>第五日</td>
                            </tr>
                            </thead>
                            <tbody class="table-body">
                            <tr ng-show="pager.total==0">
                                <td colspan="12" class="text-center">没有查询到数据！</td>
                            </tr>
                            <tr bindonce ng-repeat="foo in beans.data" ng-cloak>
                                <td bo-text="pager.start+$index+1"></td>
                                <td bo-text="foo.code"></td>
                                <td bo-text="foo.key"></td>
                                <td >
                                    <span>{{foo.date6|date:'yyyyMMdd'}}</span>
                                    <span> -- </span>
                                    <span>{{foo.businessDate|date:'yyyyMMdd'}}</span>
                                </td>
                                <td bo-text="foo.nextHigh|number:3"></td>
                                <td bo-text="foo.nextLow|number:3"></td>
                                <td bo-text="foo.isYang?'阳':'阴'" ng-class="{'red':foo.isYang,'green':!foo.isYang}"></td>
                                <td bo-text="foo.p1|number:3"></td>
                                <td bo-text="foo.p2|number:3"></td>
                                <td bo-text="foo.p3|number:3"></td>
                                <td bo-text="foo.p4|number:3"></td>
                                <td bo-text="foo.p5|number:3"></td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="list-pagination" eccrm-page="pager"></div>
</div>
</body>
<script type="text/javascript" src="<%=contextPath%>/app/stock/stock/stockDay/stockDay.js"></script>
<script type="text/javascript" src="<%=contextPath%>/app/stock/stock/stockDay/stockDay_search.js"></script>
</html>