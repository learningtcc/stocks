package com.michael.stock.fn.service.impl;

import com.michael.base.parameter.service.ParameterContainer;
import com.michael.core.beans.BeanWrapBuilder;
import com.michael.core.beans.BeanWrapCallback;
import com.michael.core.hibernate.HibernateUtils;
import com.michael.core.hibernate.validator.ValidatorUtils;
import com.michael.core.pager.PageVo;
import com.michael.stock.db.domain.DB;
import com.michael.stock.fn.bo.Fn4Bo;
import com.michael.stock.fn.dao.Fn4Dao;
import com.michael.stock.fn.domain.Fn4;
import com.michael.stock.fn.service.Fn4Service;
import com.michael.stock.fn.vo.Fn4Vo;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author Michael
 */
@Service("fn4Service")
public class Fn4ServiceImpl implements Fn4Service, BeanWrapCallback<Fn4, Fn4Vo> {
    @Resource
    private Fn4Dao fn4Dao;

    @Override
    public String save(Fn4 fn4) {
        validate(fn4);
        String id = fn4Dao.save(fn4);
        return id;
    }

    @Override
    public void update(Fn4 fn4) {
        validate(fn4);
        fn4Dao.update(fn4);
    }

    private void validate(Fn4 fn4) {
        ValidatorUtils.validate(fn4);
    }

    @Override
    public PageVo pageQuery(Fn4Bo bo) {
        PageVo vo = new PageVo();
        Long total = fn4Dao.getTotal(bo);
        vo.setTotal(total);
        if (total == null || total == 0) return vo;
        List<Fn4> fn4List = fn4Dao.pageQuery(bo);
        List<Fn4Vo> vos = BeanWrapBuilder.newInstance()
                .setCallback(this)
                .wrapList(fn4List, Fn4Vo.class);
        vo.setData(vos);
        return vo;
    }


    @Override
    public Fn4Vo findById(String id) {
        Fn4 fn4 = fn4Dao.findById(id);
        return BeanWrapBuilder.newInstance()
                .wrap(fn4, Fn4Vo.class);
    }

    @Override
    public void deleteByIds(String[] ids) {
        if (ids == null || ids.length == 0) return;
        for (String id : ids) {
            fn4Dao.deleteById(id);
        }
    }

    @Override
    public List<Fn4Vo> query(Fn4Bo bo) {
        List<Fn4> fn4List = fn4Dao.query(bo);
        List<Fn4Vo> vos = BeanWrapBuilder.newInstance()
                .setCallback(this)
                .wrapList(fn4List, Fn4Vo.class);
        return vos;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void reset() {
        int fn = 10;    // 系数范围
        long f = 1000L * 60 * 60 * 24;
        Logger logger = Logger.getLogger(Fn4ServiceImpl.class);
        logger.info(" *****************   RESET Fn4 : Start ************************** ");
        // 加载所有的日期
        final Session session = HibernateUtils.getSession(false);
        // 删除原有数据
        session.createQuery("delete from " + Fn4.class.getName()).executeUpdate();
        for (int i = 1; i < 5; i++) {
            logger.info(" *****************   RESET Fn4 : " + i + " ************************** ");
            List<Date> dates = session
                    .createQuery("select d.dbDate from " + DB.class.getName() + " d where d.type=? order by d.dbDate asc")
                    .setParameter(0, i + "")
                    .list();
            if (dates.isEmpty()) {
                continue;
            }
            int size = dates.size();
            int f1 = 0, f2 = 1, f3 = 2, f4 = 3;
            for (; f1 < size - 3; f1++) {   // 第一层游标
                Date d1 = dates.get(f1);
                for (f2 = f1 + 1; f2 < size - 2; f2++) {
                    Date d2 = dates.get(f2);
                    for (f3 = f2 + 1; f3 < size - 1; f3++) {
                        Date d3 = dates.get(f3);
                        for (f4 = f3 + 1; f4 < size; f4++) {
                            Date d4 = dates.get(f4);
                            for (int x = -fn; x <= fn; x++) {
                                long date = d1.getTime() + d2.getTime() + d3.getTime() - d4.getTime() + f * x;
                                if (date > 1577808000000L) {
                                    continue;
                                }
                                Date bk = new Date(date);
                                Fn4 fn4 = new Fn4();
                                fn4.setA1(d1);
                                fn4.setA2(d2);
                                fn4.setA3(d3);
                                fn4.setA4(d4);
                                fn4.setBk(bk);
                                fn4.setType(i);
                                fn4.setFn(x);
                                session.save(fn4);
                            }
                            session.flush();
                            session.clear();
                        }
                    }
                }
            }
        }
        logger.info(" *****************   RESET Fn4 : End ************************** ");
    }

    @Override
    public void doCallback(Fn4 fn4, Fn4Vo vo) {
        ParameterContainer container = ParameterContainer.getInstance();

    }
}