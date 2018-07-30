package com.qianfeng.analystic.mr.nu;

import com.google.common.collect.Lists;
import com.qianfeng.analystic.model.dim.StatsUserDimension;
import com.qianfeng.analystic.model.dim.value.map.TimeOutputValue;
import com.qianfeng.analystic.model.dim.value.reduce.MapWritableValue;
import com.qianfeng.analystic.mr.OutputWritterFormat;
import com.qianfeng.common.EventLogsConstant;
import com.qianfeng.common.GlobalConstants;
import com.qianfeng.util.TimeUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import java.util.List;


/**
 truncate dimension_browser;
 truncate dimension_currency_type;
 truncate dimension_date;
 truncate dimension_event;
 truncate dimension_inbound;
 truncate dimension_kpi;
 truncate dimension_location;
 truncate dimension_os;
 truncate dimension_payment_type;
 truncate dimension_platform;
 truncate event_info;
 truncate order_info;
 truncate stats_device_browser;
 truncate stats_device_location;
 truncate stats_event;
 truncate stats_hourly;
 truncate stats_inbound;
 truncate stats_order;
 truncate stats_user;
 truncate stats_view_depth;
 * @Auther: lyd
 * @Date: 2018/7/30 14:40
 * @Description:新增用户的runner类
 */
public class NewUserRunner implements Tool{
    private static final Logger logger = Logger.getLogger(NewUserRunner.class);
    private Configuration conf = new Configuration();

    public static void main(String[] args) {
        try {
            ToolRunner.run(new Configuration(),new NewUserRunner(),args);
        } catch (Exception e) {
            logger.error("运行新增用户指标失败.",e);
        }
    }

    @Override
    public void setConf(Configuration conf) {
        this.conf.addResource("query-mapping.xml");
        this.conf.addResource("output-writter.xml");
        this.conf = HBaseConfiguration.create(this.conf);
    }

    @Override
    public Configuration getConf() {
        return this.conf;
    }

    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = this.getConf();
        //设置参数到conf中
        this.setArgs(args,conf);
        //获取作业
        Job job = Job.getInstance(conf,"new_user");
        job.setJarByClass(NewUserRunner.class);

        //初始化mapper类
        //addDependencyJars : true则是本地提交集群运行，false是本地提交本地运行
        TableMapReduceUtil.initTableMapperJob(this.getScans(job),NewUserMapper.class,
                StatsUserDimension.class, TimeOutputValue.class,job,true);

        //reducer的设置
        job.setReducerClass(NewUserReudcer.class);
        job.setOutputKeyClass(StatsUserDimension.class);
        job.setOutputValueClass(MapWritableValue.class);

        //设置输出的格式类
        job.setOutputFormatClass(OutputWritterFormat.class);

        return job.waitForCompletion(true)?0:1;
    }

    /**
     * 参数处理  ,将接收到的日期存储在conf中，以供后续使用
     * @param args  如果没有传递日期，则默认使用昨天的日期
     * @param conf
     */
    private void setArgs(String[] args, Configuration conf) {
        String date = null;
        for (int i = 0;i < args.length;i++){
            if(args[i].equals("-d")){
                if(i+1 < args.length){
                    date = args[i+1];
                    break;
                }
            }
        }
        //代码到这儿，date还是null，默认用昨天的时间
        if(date == null){
            date = TimeUtil.getYesterdayDate();
        }
        //然后将date设置到时间conf中
        conf.set(GlobalConstants.RUNNING_DATE,date);
    }

    /**
     * 获取扫描的集合对象
     * @param job
     * @return
     */
    private List<Scan> getScans(Job job) {
        Configuration conf = job.getConfiguration();
        //获取运行日期
        long start = Long.valueOf(TimeUtil.parserString2Long(conf.get(GlobalConstants.RUNNING_DATE)));
        long end = start + GlobalConstants.DAY_OF_MILISECONDS;
        //获取scan对象
        Scan scan = new Scan();
        scan.setStartRow(Bytes.toBytes(start+""));
        scan.setStopRow(Bytes.toBytes(end+""));
        //定义过滤器链
        FilterList fl = new FilterList();
        //定义单列值过滤器
        fl.addFilter(new SingleColumnValueFilter(Bytes.toBytes(EventLogsConstant.HBASE_COLUMN_FAMILY),
                Bytes.toBytes(EventLogsConstant.EVENT_COLUMN_NAME_EVENT_NAME),
                CompareFilter.CompareOp.EQUAL,
                Bytes.toBytes(EventLogsConstant.EventEnum.LAUNCH.alias)));
        //设置扫描的字段
        String[] fields = {
          EventLogsConstant.EVENT_COLUMN_NAME_SERVER_TIME,
          EventLogsConstant.EVENT_COLUMN_NAME_UUID,
          EventLogsConstant.EVENT_COLUMN_NAME_PLATFORM,
          EventLogsConstant.EVENT_COLUMN_NAME_EVENT_NAME
        };
        //将扫描的字段添加到filter中
        fl.addFilter(this.getFilters(fields));
        //将scan设置表名
        scan.setAttribute(Scan.SCAN_ATTRIBUTES_TABLE_NAME,Bytes.toBytes(EventLogsConstant.HBASE_TABLE_NAME));
        //将filter设置到scan中
        scan.setFilter(fl);
        return Lists.newArrayList(scan);  //谷歌的api
    }

    /**
     * 设置扫描的列
     * @param fields
     * @return
     */
    private Filter getFilters(String[] fields) {
        int length = fields.length;
        byte[][] filters = new byte[length][];
        for (int i=0;i<length;i++){
            filters[i] = Bytes.toBytes(fields[i]);
        }
        return new MultipleColumnPrefixFilter(filters);
    }
}
