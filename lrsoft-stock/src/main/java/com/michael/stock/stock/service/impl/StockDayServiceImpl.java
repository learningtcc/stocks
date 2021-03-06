package com.michael.stock.stock.service.impl;

import com.michael.base.attachment.AttachmentProvider;
import com.michael.base.attachment.utils.AttachmentHolder;
import com.michael.base.attachment.vo.AttachmentVo;
import com.michael.base.parameter.service.ParameterContainer;
import com.michael.core.beans.BeanWrapBuilder;
import com.michael.core.beans.BeanWrapCallback;
import com.michael.core.hibernate.HibernateUtils;
import com.michael.core.hibernate.validator.ValidatorUtils;
import com.michael.core.pager.Order;
import com.michael.core.pager.PageVo;
import com.michael.core.pager.Pager;
import com.michael.stock.stock.bo.StockDayBo;
import com.michael.stock.stock.dao.StockDayDao;
import com.michael.stock.stock.domain.Stock;
import com.michael.stock.stock.domain.StockDay;
import com.michael.stock.stock.domain.StockWeek;
import com.michael.stock.stock.service.StockDayService;
import com.michael.stock.stock.service.StockRequestInstance;
import com.michael.stock.stock.service.StockWeekService;
import com.michael.stock.stock.vo.StockDayVo;
import com.michael.utils.collection.CollectionUtils;
import com.michael.utils.date.DateUtils;
import com.michael.utils.number.DoubleUtils;
import com.michael.utils.number.IntegerUtils;
import com.michael.utils.string.StringUtils;
import com.miles.stock.core.Configuration;
import com.miles.stock.sina.SinaStockAdapter;
import com.miles.stock.utils.StockUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.michael.core.hibernate.HibernateUtils.getSession;

/**
 * @author Michael
 */
@Service("stockDayService")
public class StockDayServiceImpl implements StockDayService, BeanWrapCallback<StockDay, StockDayVo> {
    @Resource
    private StockDayDao stockDayDao;

    @Resource
    private StockWeekService stockWeekService;

    @Override
    public String save(StockDay stockDay) {
        validate(stockDay);
        String id = stockDayDao.save(stockDay);
        return id;
    }

    @Override
    public void update(StockDay stockDay) {
        validate(stockDay);
        stockDayDao.update(stockDay);
    }

    private void validate(StockDay stockDay) {
        ValidatorUtils.validate(stockDay);
    }

    @Override
    public PageVo pageQuery(StockDayBo bo) {
        PageVo vo = new PageVo();
        if (Pager.getStart() == 0) {
            Long total = stockDayDao.getTotal(bo);
            vo.setTotal(total);
        }
        List<StockDay> stockDayList = stockDayDao.pageQuery(bo);
        List<StockDayVo> vos = BeanWrapBuilder.newInstance()
                .setCallback(this)
                .wrapList(stockDayList, StockDayVo.class);
        vo.setData(vos);
        return vo;
    }


    @Override
    public StockDayVo findById(String id) {
        StockDay stockDay = stockDayDao.findById(id);
        return BeanWrapBuilder.newInstance()
                .wrap(stockDay, StockDayVo.class);
    }

    @Override
    public void deleteByIds(String[] ids) {
        if (ids == null || ids.length == 0) return;
        for (String id : ids) {
            stockDayDao.deleteById(id);
        }
    }

    @Override
    public List<StockDayVo> query(StockDayBo bo) {
        List<StockDay> stockDayList = stockDayDao.query(bo);
        List<StockDayVo> vos = BeanWrapBuilder.newInstance()
                .setCallback(this)
                .wrapList(stockDayList, StockDayVo.class);
        return vos;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void reset7DayInfo(String... stocks) {
        Session session = HibernateUtils.getSession(false);
        Logger logger = Logger.getLogger(StockDayService.class);
        for (String code : stocks) {
            // 逻辑：
            // 1. 每只股票，取出最开始的4只，如果不足，则直接返回
            // 2. 取出最后一只（第4只）作为要被改变的起始数据，称作游标
            // 3.1 利用前3只股票的数据为第4只股票设置相关信息
            // 3.2 利用前6只股票的数据为第7只股票设置相关信息
            // 4. 游标往下移动，如果游标池中没有数据，则从最后一个游标的ID开始再抓取20条记录到游标池中
            // 5. 将上一个游标加入到数据池中，并将数据池的第一个元素移除
            // 关键点：数据池的大小从4到6，最后一直保持为6个，按照时间顺序排序，即最晚的交易数据在最后面
            // 利用游标遍历所有的数据
            String id = "0";
            List<StockDay> history = session.createQuery("from " + StockDay.class.getName() + " o where o.id>=? and  o.code=? order by o.id asc")
                    .setParameter(0, id)
                    .setParameter(1, code)
                    .setFirstResult(0)
                    .setMaxResults(5)
                    .list();
            final int size = history.size();
            if (size < 5) {
                continue;
            }
            // 设置前几个日期的“前一日收盘价” （这里实际上是修复错误数据的）
            history.get(1).setYesterdayClosePrice(history.get(0).getClosePrice());
            history.get(2).setYesterdayClosePrice(history.get(1).getClosePrice());
            history.get(3).setYesterdayClosePrice(history.get(2).getClosePrice());
            history.get(4).setYesterdayClosePrice(history.get(3).getClosePrice());
            List<StockDay> business = new ArrayList<>();
            business.add(history.get(4));
            history.remove(4);  // 移除最后一个
            int i = 4;
            int index = 1;
            while (true) {
                StockDay stockDay = business.get(0);    // 最后一日
                stockDay.setSeq(i++);
                // 第N日计算公式：(第N日收盘价 - 第N-1日收盘价)/第N-1日收盘价
                stockDay.setNextHigh(null);
                stockDay.setNextLow(null);
                stockDay.setYang(null);
                stockDay.setUpdown(stockDay.getClosePrice() - stockDay.getOpenPrice());
                if (stockDay.getUpdown() == 0 && stockDay.getYesterdayClosePrice() != null) {
                    stockDay.setUpdown(stockDay.getClosePrice() - stockDay.getYesterdayClosePrice());
                }

                // 设置3K数据
                StockDay yesterday = history.get(history.size() - 1);
                stockDay.setKey(yesterday.getKey().substring(1) + (stockDay.getUpdown() > 0 ? "1" : "0"));
                stockDay.setKey3(yesterday.getKey3().substring(1) + (stockDay.getUpdown() > 0 ? "1" : "0"));
                stockDay.setDate3(history.get(index + 1).getBusinessDate());
                stockDay.setD1((history.get(index).getClosePrice() - history.get(index).getYesterdayClosePrice()) / history.get(index).getYesterdayClosePrice());
                stockDay.setD2((history.get(index + 1).getClosePrice() - history.get(index + 1).getYesterdayClosePrice()) / history.get(index + 1).getYesterdayClosePrice());
                stockDay.setD3((history.get(index + 2).getClosePrice() - history.get(index + 2).getYesterdayClosePrice()) / history.get(index + 2).getYesterdayClosePrice());
                stockDay.setYesterdayClosePrice(yesterday.getClosePrice());

                // 四日、七日的高低
                yesterday.setNextHigh((stockDay.getHighPrice() - yesterday.getClosePrice()) / yesterday.getClosePrice());
                yesterday.setNextLow((stockDay.getLowPrice() - yesterday.getClosePrice()) / yesterday.getClosePrice());
                // 四七日阴阳
                if (stockDay.getClosePrice() - stockDay.getOpenPrice() == 0) {
                    yesterday.setYang(stockDay.getClosePrice() > DoubleUtils.add(stockDay.getYesterdayClosePrice()));
                } else {
                    yesterday.setYang(stockDay.getClosePrice() > stockDay.getOpenPrice());
                }
                if (history.size() < 8) {
                    index++;
                    session.update(yesterday);
                    session.update(stockDay);
                    history.add(stockDay);
                    business.remove(0);
                    if (business.isEmpty()) {
                        // 加载20条继续执行
                        business.addAll(session.createQuery("from " + StockDay.class.getName() + " o where o.id>? and  o.code=? order by o.id asc")
                                .setParameter(0, stockDay.getId())
                                .setParameter(1, code)
                                .setFirstResult(0)
                                .setMaxResults(20)
                                .list());
                    }
                    if (business.isEmpty()) {
                        break;
                    }
                    continue;
                }

                // 设置6k数据
                // 第1日
                StockDay sd1 = history.get(1);
                stockDay.setDate6(sd1.getBusinessDate());
                stockDay.setP1((history.get(1).getClosePrice() - history.get(1).getYesterdayClosePrice()) / history.get(1).getYesterdayClosePrice());
                stockDay.setP2((history.get(2).getClosePrice() - history.get(2).getYesterdayClosePrice()) / history.get(2).getYesterdayClosePrice());
                stockDay.setP3((history.get(3).getClosePrice() - history.get(3).getYesterdayClosePrice()) / history.get(3).getYesterdayClosePrice());
                stockDay.setP4((history.get(4).getClosePrice() - history.get(4).getYesterdayClosePrice()) / history.get(4).getYesterdayClosePrice());
                stockDay.setP5((history.get(5).getClosePrice() - history.get(5).getYesterdayClosePrice()) / history.get(5).getYesterdayClosePrice());
                stockDay.setP6((history.get(6).getClosePrice() - history.get(6).getYesterdayClosePrice()) / history.get(6).getYesterdayClosePrice());
                session.update(yesterday);
                session.update(stockDay);
                // 调整集合顺序
                history.remove(0);
                history.add(stockDay);
                business.remove(0);

                if (business.isEmpty()) {
                    // 加载20条继续执行
                    business = session.createQuery("from " + StockDay.class.getName() + " o where o.id>? and  o.code=? order by o.id asc")
                            .setParameter(0, stockDay.getId())
                            .setParameter(1, code)
                            .setFirstResult(0)
                            .setMaxResults(20)
                            .list();
                }
                if (i % 10 == 0) {
                    session.flush();
                    session.clear();
                }
                if (business.isEmpty()) {
                    break;
                }
            }
            logger.info(String.format("更新%s结束，共计%d条", code, i));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> syncStockBusiness(String... stocks) {

        if (stocks == null || stocks.length == 0) {
            return null;
        }
        if (Configuration.getInstance().getStockAdapter() == null) {
            Configuration.getInstance().setStockAdapter(new SinaStockAdapter());
        }
        String content = StockRequestInstance.getInstance().getStockRequest().get(stocks);
        String stockResult[] = content.split(";");
        List<com.miles.stock.domain.Stock> stockList = StockUtils.wrapToStock(stockResult);
        if (stockList.isEmpty()) {
            return null;
        }
        Date date = DateUtils.parse(stockList.get(0).getDate());
        Session session = HibernateUtils.getSession(false);
        Logger logger = Logger.getLogger(StockDayServiceImpl.class);
        int i = 0;
        for (com.miles.stock.domain.Stock s : stockList) {
            // 如果该条数据已经有交易数据，则跳过
            final String code = s.getCode();
            logger.info(String.format("处理今日股票%s交易数据:%s", code, stockResult[i++]));

            // 如果股票停盘，则跳过
            if (s.getClosePrice() == 0 || s.getOpenPrice() == 0) {
                logger.info(String.format("股票%s(%s)停盘，不做记录!", s.getCode(), s.getName()));
                continue;
            }
            // 获取今天之前6天的交易数据
            List<StockDay> history = session.createQuery("from " + StockDay.class.getName() + " sd where sd.businessDate<? and sd.code=? order by sd.businessDate desc")
                    .setParameter(0, date)
                    .setParameter(1, code)
                    .setFirstResult(0)
                    .setMaxResults(6)
                    .list();
            StockDay stockDay = new StockDay();
            stockDay.setCode(code);
            stockDay.setName(s.getName().replaceAll("\\s+", ""));
            stockDay.setBusinessDate(date);
            stockDay.setHighPrice(Double.parseDouble(s.getTodayHighPrice().toString()));
            stockDay.setLowPrice(Double.parseDouble(s.getTodayLowPrice().toString()));
            stockDay.setOpenPrice(Double.parseDouble(s.getOpenPrice().toString()));
            stockDay.setClosePrice(Double.parseDouble(s.getClosePrice().toString()));
            stockDay.setYesterdayClosePrice(Double.parseDouble(s.getYesterdayClosePrice().toString()));
            stockDay.setUpdown(stockDay.getClosePrice() - stockDay.getOpenPrice());
            if (stockDay.getUpdown() == 0 && stockDay.getYesterdayClosePrice() != null) {
                stockDay.setUpdown(stockDay.getClosePrice() - stockDay.getYesterdayClosePrice());
            }
            stockDay.setKey((stockDay.getUpdown() > 0 ? "000001" : "000000"));
            stockDay.setKey3((stockDay.getUpdown() > 0 ? "001" : "000"));
            stockDay.setSeq(history.size());
            // 第N日计算公式：(第N日收盘价 - 第N-1日收盘价)/第N-1日收盘价
            int size = history.size();
            if (size == 6) {
                // 6日时间&组合
                final StockDay yesterday = history.get(0);
                stockDay.setSeq(IntegerUtils.add(yesterday.getSeq(), 1));
                stockDay.setYesterdayClosePrice(yesterday.getClosePrice());
                stockDay.setDate6(history.get(4).getBusinessDate());
                stockDay.setKey(yesterday.getKey().substring(1) + (stockDay.getUpdown() > 0 ? "1" : "0"));

                // 3日时间&组合
                stockDay.setDate3(history.get(1).getBusinessDate());
                stockDay.setKey3(yesterday.getKey3().substring(1) + (stockDay.getUpdown() > 0 ? "1" : "0"));

                // 第N日计算公式：(第N日收盘价 - 第N-1日收盘价)/第N-1日收盘价
                // 第1日
                StockDay sd1 = history.get(4);
                Double d1 = (sd1.getClosePrice() - sd1.getYesterdayClosePrice()) / sd1.getYesterdayClosePrice();
                stockDay.setP1(d1);
                // 第2日
                StockDay sd2 = history.get(3);
                Double d2 = (sd2.getClosePrice() - sd2.getYesterdayClosePrice()) / sd2.getYesterdayClosePrice();
                stockDay.setP2(d2);
                // 第3日
                StockDay sd3 = history.get(2);
                Double d3 = (sd3.getClosePrice() - sd3.getYesterdayClosePrice()) / sd3.getYesterdayClosePrice();
                stockDay.setP3(d3);
                // 第4日
                StockDay sd4 = history.get(1);
                Double d4 = (sd4.getClosePrice() - sd4.getYesterdayClosePrice()) / sd4.getYesterdayClosePrice();
                stockDay.setP4(d4);
                // 第5日
                StockDay sd5 = history.get(0);
                Double d5 = (sd5.getClosePrice() - sd5.getYesterdayClosePrice()) / sd5.getYesterdayClosePrice();
                stockDay.setP5(d5);
                // 第6日
                Double d6 = (stockDay.getClosePrice() - stockDay.getYesterdayClosePrice()) / stockDay.getYesterdayClosePrice();
                stockDay.setP6(d6);

                // 第七日高
                yesterday.setNextHigh((stockDay.getHighPrice() - yesterday.getClosePrice()) / yesterday.getClosePrice());
                // 第七日低
                yesterday.setNextLow((stockDay.getLowPrice() - yesterday.getClosePrice()) / yesterday.getClosePrice());

                if (StringUtils.isNotEmpty(yesterday.getId())) {
                    session.update(yesterday);
                }

                // 七日阴阳
                if (stockDay.getClosePrice() - stockDay.getOpenPrice() == 0) {
                    yesterday.setYang(stockDay.getClosePrice() > stockDay.getYesterdayClosePrice());
                } else {
                    yesterday.setYang(stockDay.getClosePrice() > stockDay.getOpenPrice());
                }
            }

            stockDayDao.save(stockDay);

            // 触发周K数据的同步
            stockWeekService.add(code);

            session.flush();
            session.clear();
        }
        return null;
    }

    public PageVo result3(StockDayBo bo) {
        PageVo vo = new PageVo();
        int start = IntegerUtils.add(Pager.getStart());
        int limit = IntegerUtils.add(Pager.getLimit());
        Session session = HibernateUtils.getSession(false);
        String coreSql = " from result_day3 rd " +
                " JOIN (select concat(d.code,':',d.s_key3) as name from stock_day d join (select max(businessDate) businessDate from stock_day) t on t.businessDate=d.businessDate where d.nextHigh is null ) t " +
                " on rd.name=t.name ";
        List<Object> params = new ArrayList<>();
        if (bo != null) {
            if (StringUtils.isNotEmpty(bo.getCode())) {
                coreSql += " and code=? ";
                params.add(bo.getCode());
            }
            if (StringUtils.isNotEmpty(bo.getKey3())) {
                coreSql += " and key1=? ";
                params.add(bo.getKey3());
            }
        }
        Query totalQuery = session.createSQLQuery("select count(rd.name) " + coreSql);
        if (Pager.getOrder() != null && Pager.getOrder().hasNext()) {
            Order o = Pager.getOrder().next();
            coreSql += " order by rd." + o.getName() + (o.isReverse() ? " desc " : " asc ");
        } else {
            coreSql += " order by rd.key1 asc ";
        }
        Query query = session.createSQLQuery("select rd.* " + coreSql);
        if (CollectionUtils.isNotEmpty(params)) {
            for (int i = 0; i < params.size(); i++) {
                totalQuery.setParameter(i, params.get(i));
                query.setParameter(i, params.get(i));
            }
        }
        query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        BigInteger bigInteger = (BigInteger) totalQuery.uniqueResult();
        if (bigInteger == null || bigInteger.intValue() == 0) {
            vo.setTotal(0L);
            return vo;
        }
        vo.setTotal(bigInteger.longValue());
        query.setFirstResult(start);
        query.setMaxResults(limit);
        List<Map<String, Object>> data = query.list();
        vo.setData(data);
        return vo;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageVo result6(StockDayBo bo) {
        PageVo vo = new PageVo();
        int start = IntegerUtils.add(Pager.getStart());
        int limit = IntegerUtils.add(Pager.getLimit());
        Session session = HibernateUtils.getSession(false);
        String coreSql = " from result_day6 rd" +
                " JOIN (select concat(d.code,':',d.s_key) as name from stock_day d join (select max(businessDate) businessDate from stock_day) t on t.businessDate=d.businessDate where d.nextHigh is null ) t\n" +
                " on rd.name=t.name ";
        List<Object> params = new ArrayList<>();
        if (bo != null) {
            if (StringUtils.isNotEmpty(bo.getCode())) {
                coreSql += " and code=? ";
                params.add(bo.getCode());
            }
            if (StringUtils.isNotEmpty(bo.getKey())) {
                coreSql += " and key1=? ";
                params.add(bo.getKey());
            }
        }
        Query totalQuery = session.createSQLQuery("select count(rd.name) " + coreSql);
        if (Pager.getOrder() != null && Pager.getOrder().hasNext()) {
            Order o = Pager.getOrder().next();
            coreSql += " order by rd." + o.getName() + (o.isReverse() ? " desc " : " asc ");
        } else {
            coreSql += " order by rd.key1 asc ";
        }
        Query query = session.createSQLQuery("select rd.* " + coreSql);
        if (CollectionUtils.isNotEmpty(params)) {
            for (int i = 0; i < params.size(); i++) {
                totalQuery.setParameter(i, params.get(i));
                query.setParameter(i, params.get(i));
            }
        }
        query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        BigInteger bigInteger = (BigInteger) totalQuery.uniqueResult();
        if (bigInteger == null || bigInteger.intValue() == 0) {
            vo.setTotal(0L);
            return vo;
        }
        vo.setTotal(bigInteger.longValue());
        query.setFirstResult(start);
        query.setMaxResults(limit);
        List<Map<String, Object>> data = query.list();
        vo.setData(data);
        return vo;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> report3(StockDayBo bo) {
        Session session = HibernateUtils.getSession(false);
        String sql = "select s_key3 as key1,code," +
                "sum(case isYang when 1 then 1 else 0 end) as yang," +
                "sum(nextHigh) as nextHigh,sum(nextLow) as nextLow,count(id) as counts " +
                "from stock_day where nextHigh is not null ";
        List<Object> params = new ArrayList<>();
        if (bo.getBusinessDateGe() != null) {
            sql += " and businessDate>= ? ";
            params.add(bo.getBusinessDateGe());
        }
        if (bo.getBusinessDateLt() != null) {
            sql += " and businessDate<? ";
            params.add(bo.getBusinessDateLt());
        }
        if (StringUtils.isNotEmpty(bo.getCode())) {
            sql += " and code=? ";
            params.add(bo.getCode());
        }
        if (StringUtils.isNotEmpty(bo.getKey3())) {
            sql += " and s_key3=? ";
            params.add(bo.getKey3());
        }
        sql += " group by s_key3,code ";
        sql = "select t.*,t.nextHigh/t.counts as avgHigh,t.nextLow/t.counts as avgLow,t.yang/t.counts as per from (" + sql + ") t ";
        if (Pager.getOrder() != null && Pager.getOrder().hasNext()) {
            Order o = Pager.getOrder().next();
            sql += " order by " + o.getName() + (o.isReverse() ? " desc " : " asc ");
        } else {
            sql += " order by t.key1 asc ";
        }
        Query query = session.createSQLQuery(sql);
        int index = 0;
        for (Object o : params) {
            query.setParameter(index++, o);
        }
        return query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .setFirstResult(IntegerUtils.add(Pager.getStart()))
                .setMaxResults(IntegerUtils.add(Pager.getLimit()))
                .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> report6(StockDayBo bo) {
        Session session = HibernateUtils.getSession(false);
        String sql = "select s_key as key1,code," +
                "sum(case isYang when 1 then 1 else 0 end) as yang," +
                "sum(nextHigh) as nextHigh,sum(nextLow) as nextLow,count(id) as counts " +
                "from stock_day where  nextHigh is not null ";
        List<Object> params = new ArrayList<>();
        if (bo.getBusinessDateGe() != null) {
            sql += " and businessDate>= ? ";
            params.add(bo.getBusinessDateGe());
        }
        if (bo.getBusinessDateLt() != null) {
            sql += " and businessDate<? ";
            params.add(bo.getBusinessDateLt());
        }
        if (StringUtils.isNotEmpty(bo.getCode())) {
            sql += " and code=? ";
            params.add(bo.getCode());
        }
        if (StringUtils.isNotEmpty(bo.getKey())) {
            sql += " and s_key=? ";
            params.add(bo.getKey());
        }
        sql += " group by s_key,code ";
        sql = "select t.*,t.nextHigh/t.counts as avgHigh,t.nextLow/t.counts as avgLow,t.yang/t.counts as per from (" + sql + ") t ";
        if (Pager.getOrder() != null && Pager.getOrder().hasNext()) {
            Order o = Pager.getOrder().next();
            sql += " order by " + o.getName() + (o.isReverse() ? " desc " : " asc ");
        } else {
            sql += " order by t.key1 asc ";
        }
        Query query = session.createSQLQuery(sql);
        int index = 0;
        for (Object o : params) {
            query.setParameter(index++, o);
        }
        return query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .setFirstResult(IntegerUtils.add(Pager.getStart()))
                .setMaxResults(IntegerUtils.add(Pager.getLimit()))
                .list();
    }


    @Override
    public Date lastDay() {
        return (Date) HibernateUtils.getSession(false)
                .createQuery("select distinct max(o.businessDate) from " + StockDay.class.getName() + " o ")
                .setMaxResults(1)
                .uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public void importData(List<String> attachmentIds) {
        Logger logger = Logger.getLogger(StockDayServiceImpl.class);
        Assert.notEmpty(attachmentIds, "数据导入失败!数据文件不能为空，请重试!");
        Session session = getSession(false);
        int index = 0;
        int x = 1;
        List<Object[]> stockNames = session.createQuery("select s.code,s.name from " + Stock.class.getName() + " s ")
                .list();
        Map<String, String> stockMap = new HashMap<>();
        for (Object[] o : stockNames) {
            stockMap.put((String) o[0], (String) o[1]);
        }
        for (String id : attachmentIds) {
            AttachmentVo vo = AttachmentProvider.getInfo(id);
            Assert.notNull(vo, "附件已经不存在，请刷新后重试!");
            final File file = AttachmentHolder.newInstance().getTempFile(id);
            long start = System.currentTimeMillis();
            logger.info(String.format("开始导入数据%d/%d....", x++, attachmentIds.size()));
            try {
                List<String> content = FileUtils.readLines(file, "gbk");
                final int size = content.size();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                String titles[] = content.get(0).split("\\s+");
                String code = titles[0];
                String name = stockMap.get(code);
                if (StringUtils.isEmpty(name)) {
                    name = titles[1];
                }
                // 清空这支股票的历史数据
                session.createQuery("delete from " + StockDay.class.getName() + " s where s.code=?")
                        .setParameter(0, code)
                        .executeUpdate();
                session.createQuery("delete from " + StockWeek.class.getName() + " s where s.code=?")
                        .setParameter(0, code)
                        .executeUpdate();
                String key = "000000";
                Double closePrice = null;
                for (int i = 1; i < size; i++) {
                    String[] arr = content.get(i).split(";");
                    if (arr.length != 7) {
                        continue;
                    }
                    StockDay stockDay = new StockDay();
                    stockDay.setCode(code);
                    stockDay.setName(name);
                    stockDay.setBusinessDate(sdf.parse(arr[0]));
                    stockDay.setOpenPrice(Double.parseDouble(arr[1]));
                    stockDay.setHighPrice(Double.parseDouble(arr[2]));
                    stockDay.setLowPrice(Double.parseDouble(arr[3]));
                    stockDay.setClosePrice(Double.parseDouble(arr[4]));
                    stockDay.setUpdown(stockDay.getClosePrice() - stockDay.getOpenPrice());
                    if (stockDay.getUpdown() == 0 && stockDay.getYesterdayClosePrice() != null) {
                        stockDay.setUpdown(stockDay.getClosePrice() - stockDay.getYesterdayClosePrice());
                    }
                    key = key.substring(1) + (stockDay.getUpdown() > 0 ? "1" : "0");
                    stockDay.setKey(key);
                    stockDay.setKey3(key.substring(3));
                    if (i > 1) {
                        stockDay.setYesterdayClosePrice(closePrice);
                        closePrice = stockDay.getClosePrice();
                    }
                    session.save(stockDay);
                    index++;
                    if (index % 20 == 0) {
                        session.flush();
                        session.clear();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            logger.info(String.format("导入数据成功,用时(%d)s，共导入%d条数据....", (System.currentTimeMillis() - start) / 1000, index));
        }
    }

    @Override
    public void doCallback(StockDay stockDay, StockDayVo vo) {
        ParameterContainer container = ParameterContainer.getInstance();

    }
}
