<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>登录日志列表</title>
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
        .block table tr td:last-child {
            width: auto;
        }
    </style>
</head>
<body>
<div class="main condition-row-1" ng-app="base.log.loginLog.list" ng-controller="Ctrl">
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
                        <div class="item w300">
                            <div class="form-label w100">
                                <label>登录IP:</label>
                            </div>
                            <input type="text" class="w200" ng-model="condition.ip"
                                   maxlength="20"/>
                        </div>
                        <div class="item w600">
                            <div class="form-label w100">
                                <label>登录时间:</label>
                            </div>
                            <div class="w200 pr">
                                <input type="text" class="w200" ng-model="condition.loginTimeGe"
                                       eccrm-my97="{dateFmt:'yyyy-MM-dd HH:mm:ss'}" readonly
                                       placeholder="点击选择时间"/>
                                <span class="add-on">
                                    <i class="icons icon clock" ng-click="condition.loginTimeGe=null" title="点击清除"></i>
                                </span>
                            </div>
                            <div class="w100 text-center" style="width: 40px;">-</div>
                            <div class="w200 pr">
                                <input type="text" class="w200" ng-model="condition.loginTimeLt"
                                       eccrm-my97="{dateFmt:'yyyy-MM-dd HH:mm:ss'}" readonly
                                       placeholder="点击选择时间"/>
                                <span class="add-on">
                                    <i class="icons icon clock" ng-click="condition.loginTimeLt=null" title="点击清除"></i>
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
                    <span>登录日志列表</span>
                </div>
                <span class="header-button">
                </span>
            </div>
            <div class="block-content">
                <div class="content-wrap">
                    <div class="table-responsive panel panel-table">
                        <table class="table table-striped table-hover">
                            <thead class="table-header">
                            <tr>
                                <td style="width: 20px;">序号</td>
                                <td style="width: 120px;">登录人</td>
                                <td style="width: 120px;">登录IP</td>
                                <td style="width: 120px;">登录时间</td>
                                <td style="width: 120px;">退出时间</td>
                                <td style="width: 120px;">退出方式</td>
                                <td>备注</td>
                            </tr>
                            </thead>
                            <tbody class="table-body">
                            <tr ng-show="!beans || !beans.total">
                                <td colspan="7" class="text-center">没有查询到数据！</td>
                            </tr>
                            <tr bindonce ng-repeat="foo in beans.data" ng-cloak>
                                <td bo-text="pager.start+$index+1"></td>
                                <td bo-text="foo.creatorName"></td>
                                <td bo-text="foo.ip"></td>
                                <td bo-text="foo.loginTime|eccrmDatetime"></td>
                                <td bo-text="foo.logoutTime|eccrmDatetime"></td>
                                <td bo-text="foo.type"></td>
                                <td bo-text="foo.description" auto-length="true"></td>
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
<script type="text/javascript"
        src="<%=contextPath%>/app/base/log/loginLog/loginLog.js"></script>
<script type="text/javascript"
        src="<%=contextPath%>/app/base/log/loginLog/loginLog_list.js"></script>
</html>