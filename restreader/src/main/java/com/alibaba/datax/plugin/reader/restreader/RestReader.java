package com.alibaba.datax.plugin.reader.restreader;

import cn.hutool.http.Method;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.statistics.PerfRecord;
import com.alibaba.datax.common.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * restreader主要通过发起rest请求获取数据并支持自动获取全部分页数据，返回的数据格式目前支持的是json格式
 * 1. 只支持get和post请求
 * 2. 自动并发分页请求（开启设置“pagination":true），任务数为分页
 */
public class RestReader extends Reader {
	public static class Job extends Reader.Job {
		private static final Logger LOG = LoggerFactory.getLogger(RestReader.class);

		private RestRequest restRequest;
		private Configuration configuration;

		/**
		 * Job对象初始化工作，此时可以通过super.getPluginJobConf()获取与本插件相关的配置。
		 * 读插件获得配置中reader部分，写插件获得writer部分。
		 */
		@Override
		public void init() {
			this.configuration = this.getPluginJobConf();
			String method = configuration.getString("method", "GET").toUpperCase();
			if("GET".equals(method)){
				restRequest = new GetRestRequest(configuration);
			}
			if("POST".equals(method)){
				restRequest = new PostRestRequest(configuration);
			}
		}

		/**
		 * 全局准备工作
		 */
		@Override
		public void prepare() {
		}

		/**
		 * 全局的后置工作
		 */
		@Override
		public void post() {
		}

		/**
		 * Job对象自身的销毁工作
		 */
		@Override
		public void destroy() {
		}

		/**
		 * 如果开启了分页("pagination":true),则按照分页总页数进行任务的拆分。
		 * 总页数是根据配置的分页请求返回的总记录和分页记录计算得出。
		 * 后期考虑支持多种方式，比如有的分页可能不返回总记录
		 *
		 * @param adviceNumber
		 *
		 *            着重说明下，adviceNumber是框架建议插件切分的任务数，插件开发人员最好切分出来的任务数>=
		 *            adviceNumber。<br>
		 * <br>
		 *            之所以采取这个建议是为了给用户最好的实现，例如框架根据计算认为用户数据存储可以支持100个并发连接，
		 *            并且用户认为需要100个并发。 此时，插件开发人员如果能够根据上述切分规则进行切分并做到>=100连接信息，
		 *            DataX就可以同时启动100个Channel，这样给用户最好的吞吐量 <br>
		 *            例如用户同步一张Mysql单表，但是认为可以到10并发吞吐量，插件开发人员最好对该表进行切分，比如使用主键范围切分，
		 *            并且如果最终切分任务数到>=10，我们就可以提供给用户最大的吞吐量。 <br>
		 * <br>
		 *            当然，我们这里只是提供一个建议值，Reader插件可以按照自己规则切分。但是我们更建议按照框架提供的建议值来切分。 <br>
		 * <br>
		 *            对于ODPS写入OTS而言，如果存在预排序预切分问题，这样就可能只能按照分区信息切分，无法更细粒度切分，
		 *            这类情况只能按照源头物理信息切分规则切分。 <br>
		 * <br>
		 *
		 *
		 * @return
		 */
		@Override
		public List<Configuration> split(int adviceNumber) {
			Boolean pagination = configuration.getBool("pagination", false);
			if(!pagination){
				return Arrays.asList(this.configuration);
			}
			//每个分页一个task
			String resp = restRequest.sendHttpRequest();
			restRequest.calculateTotalPages(resp);
			int totalPages = restRequest.getTotalPages();
			LOG.info("totalPages: {}, pageIndex: {}", totalPages, restRequest.getPageIndex());

			if(restRequest.getPageIndex() == 1){
				//防止有的开始页从1算起
				totalPages += 1;
			}

			List<Configuration> paginationConfigs = new ArrayList<>();
			for(int i = restRequest.getPageIndex(); i < totalPages; i++) {
				Configuration config = this.configuration.clone();
				//替换pageNo
				if(Method.GET.equals(restRequest.getMethod())){
					//替换url地址中pageNo参数
					String url = config.getString("url", "");
					String pageNoParam = config.getString("pageNoParam");
					String pageNoRegex = pageNoParam + "=" + "(\\d+)";
//					LOG.info("pageNoRegex: {}", pageNoRegex);
					String newUrl = url.replaceAll(pageNoRegex, pageNoParam + "=" + i);
					config.set("url", newUrl);
//					LOG.info("old url: {}, new url: {}", url, newUrl);
				}
				if(Method.POST.equals(restRequest.getMethod())){
					//替换body里面的pageNo参数
					String body = config.getString("body", "");
					String pageNoParam = config.getString("pageNoParam");
					String pageNoRegex = "\"" + pageNoParam + "\"" + ":" + "(\\d+)";
					String newBody = body.replaceAll(pageNoRegex, "\"" + pageNoParam + "\"" + ":" + i);
					config.set("body", newBody);
//					LOG.info("old body: {}, new body: {}", body, newBody);
				}
				paginationConfigs.add(config);
			}
			return paginationConfigs;
		}

	}

	public static class Task extends Reader.Task {
		private static Logger LOG = LoggerFactory.getLogger(Task.class);
		private RestRequest restRequest;
		private Configuration configuration;


		/**
		 * Task对象的初始化。此时可以通过super.getPluginJobConf()获取与本Task相关的配置。
		 * 这里的配置是Job的split方法返回的配置列表中的其中一个。
		 */
		@Override
		public void init() {
			this.configuration = this.getPluginJobConf();
			String method = configuration.getString("method", "GET").toUpperCase();
			if("GET".equals(method)){
				restRequest = new GetRestRequest(configuration);
			}
			if("POST".equals(method)){
				restRequest = new PostRestRequest(configuration);
			}
		}

		/**
		 * 局部的准备工作
		 */
		@Override
		public void prepare() {

		}



		/**
		 * 局部的后置工作
		 */
		@Override
		public void post() {

		}

		/**
		 * Task象自身的销毁工作
		 */
		@Override
		public void destroy() {
		}

		/**
		 * 发起http请求，并将数据写入到RecordSender中。
		 * RecordSender会把数据写入连接Reader和Writer的缓存队列
		 * @param recordSender
		 */
		@Override
		public void startRead(RecordSender recordSender) {
			PerfRecord queryPerfRecord = new PerfRecord(super.getTaskGroupId(), super.getTaskId(), PerfRecord.PHASE.SQL_QUERY);
			queryPerfRecord.start();
			String resp = restRequest.sendHttpRequest();
			restRequest.send2Writer(recordSender, resp);
			queryPerfRecord.end();
		}


	}


}
