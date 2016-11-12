package com.michael.stock.stock.schedule;

import com.michael.core.SystemContainer;
import com.michael.core.hibernate.HibernateUtils;
import com.michael.core.pool.ThreadPool;
import com.michael.stock.stock.domain.Stock;
import com.michael.stock.stock.service.StockService;
import com.michael.utils.number.IntegerUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 同步股票当天的交易记录的定时器（下午3:30）
 *
 * @author Michael
 */
@Component
public class StockBusinessSchedule {

    @Scheduled(cron = "0 30 15 * * ?")
    @SuppressWarnings("unchecked")
    public void execute() {
        Logger logger = Logger.getLogger(StockBusinessSchedule.class);
        logger.info("****************** 同步股票交易数据:start ******************");
        // 获取所有的股票编号
        try (Session session = HibernateUtils.openSession()) {
            List<String> codes = session.createQuery("select o.code from " + Stock.class.getName() + " o ")
                    .list();
            int size = codes.size();
            long times = IntegerUtils.times(size, 10);
            for (int i = 0; i < times; i++) {
                int last = (i + 1) * 10;
                if (last > size) {
                    last = size;
                }
                ThreadPool.getInstance().execute(new StockBusinessThread(codes.subList(i * 10, last).toArray(new String[]{})));
            }
        }
        StockService stockService = SystemContainer.getInstance().getBean(StockService.class);
        stockService.syncStock();
        logger.info("****************** 同步股票交易数据:end ******************");
    }
}