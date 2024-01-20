# DataX RestReader 说明


------------

## 1 快速介绍

RestReader是基于DataX开发的rest请求的read插件，提供了读取rest请求数据的能力，并支持自动并发请求分页。
在底层实现上，通过首次rest请求获取到总记录数和页记录数算出总分页数。
总分页数即DataX的任务数，由DataX调度并发发起请求。


## 2 功能与限制

1. 目前仅支持get和post请求

2. 目前http请求响应仅支持json

3. post请求不支持form方式

4. 数据格式：multiData 多条数据，返回为数组  oneData 单条数据，返回为Map。当前版本仅支持multiData



## 3 功能说明


### 3.1 配置样例

#### 3.1.1 Get请求样例

```json
{
	"job": {
		"setting": {
			"speed": {
				"channel":1
			},
			"errorLimit": {
				"record": 0,
				"percentage": 0.02
			}
		},
		"content": [
			{
				"reader": {
					"name": "restreader",
					"parameter": {
						"url": "http://10.68.6.40/usmp-backend/card/records?pageNo=1&pageIndex=0&pageSize=10&timeRangeType=0&doorId=",
						"method": "get",
						"timeout": 10,
						"parameters": "",
						"dataPath": "rows",
						"pagination":true,
						"totalParamPath":"total",
						"startIndexParam":"pageIndex",
						"pageSizeParam":"pageSize",
						"pageNoParam":"pageNo",
						"customHeader": {
							"Jwt-Token": "eyJhbGciOiJSUzI1NiJ9.eyJhdXRoVXNlckRldGFpbHMiOiJ7XCJ1c2VyXCI6IHtcImNvZGVcIjogXCIwMDA4XCJ9fSIsInN1YiI6IlVzbXBBZG1pbiIsImRlZmF1bHRHcmFudFNjb3BlIjoiIiwiYW1yIjoibWZhIiwiaXNzIjoiaHR0cDovL2Rldi5jdHNwLmtlZGFjb20uY29tL2Nsb3VkLXJiYWMiLCJhdXRoVGltZSI6MTcwODIyMzUzNDk5NiwiYWNjZXNzVG9rZW4iOiJtakNBK2Q1QklsVmp6OXczTDgyRTB2aFVlVkRweVExNlV4SUJ0M0g3aTd0blNzZWNGY2s1QzBGQjlzSDFFam5LV21wZko4QlVvTFFIMDhDQkluaVVVQXhWaWRKdkNTNEU5b0pncjZwdmJORT0iLCJub25jZSI6bnVsbCwiYXVkIjoidHktdXNtcC1iYWNrZW5kIiwiYWNyIjoidXJuOm1hY2U6aW5jb21tb246aWFwOnNpbHZlciIsImF6cCI6IiIsInRlbmFudElkIjpudWxsLCJleHAiOjE3MDgyMjM1MzQsImlhdCI6MTcwNTYzMTUzNH0.pZvVozEi595HMt5A5Sj_WmCvNrubZOe0TFnycYF3MrmQbjcF_dDFLlYQh55Po_Zd2eJBwbm9oyO9HNyMsTOq0A"
						},
						"column" : [
							{
								"value": "id",
								"type": "long"
							},
							{
								"value": "gbid",
								"type": "string"
							}
						]
					}
				},
				"writer": {
					"name": "mysqlwriter",
					"parameter": {
						"writeMode": "insert",
						"username": "root",
						"password": "root",
						"column": [
							"type",
							"name"
						],
						"connection": [
							{
								"jdbcUrl": "jdbc:mysql://127.0.0.1:3306/datax?characterEncoding=utf-8",
								"table": [
									"test"
								]
							}
						]
					}
				}
			}
		]
	}
}
```
<br/>

#### 3.1.2 Post请求样例

```json
{
	"job": {
		"setting": {
			"speed": {
				"channel":1
			},
			"errorLimit": {
				"record": 0,
				"percentage": 0.02
			}
		},
		"content": [
			{
				"reader": {
					"name": "restreader",
					"parameter": {
						"url": "http://transit.devdolphin.com/ga-pcs-bs-back/patrol/case_info_alarm/listAlarm",
						"method": "post",
						"dataPath": "result.content",
						"pagination":true,
						"totalParamPath":"result.totalElements",
						"startIndexParam":"pageIndex",
						"pageSizeParam":"pageSize",
						"pageNoParam":"pageNo",
						"body": "{\"caseNo\":\"\",\"caseName\":\"\",\"status\":\"\",\"deptCode\":\"\",\"acceptPeople\":\"\",\"closePoliceNo\":\"\",\"type\":\"0\",\"expireDateStart\":\"\",\"expireDateEnd\":\"\",\"pageNo\":0,\"pageSize\":20}",
						"customHeader": {
							"Jwt-Token": "eyJhbGciOiJSUzI1NiJ9.eyJhdXRoVXNlckRldGFpbHMiOiJ7XCJ1c2VyXCI6IHtcImNvZGVcIjogXCI4MDAwMDRcIn19Iiwic3ViIjoicWhwY3MiLCJkZWZhdWx0R3JhbnRTY29wZSI6IiIsImFtciI6Im1mYSIsImlzcyI6Imh0dHA6Ly9kZXYuY3RzcC5rZWRhY29tLmNvbS9jbG91ZC1yYmFjIiwiYXV0aFRpbWUiOjE3MDU3MjExODQ4OTksImFjY2Vzc1Rva2VuIjoibWpDQStkNUJJbFZqejl3M0w4MkUwa0JBY21Wb0x2SUJXTUxYbGVDU29tK25nMTVtUnlGY1NlVFRRSElpdHdrUnljc21XK0lFYzZOL1FSTmMveXZsS1JZWk5hYXJ4VEpyc2d0eWtMSUYvY1k9Iiwibm9uY2UiOm51bGwsImF1ZCI6ImdhLXBjcy1icy1iYWNrIiwiYWNyIjoidXJuOm1hY2U6aW5jb21tb246aWFwOnNpbHZlciIsImF6cCI6IiIsInRlbmFudElkIjpudWxsLCJleHAiOjE3MDU3MjExODQsImlhdCI6MTcwNTYzNDc4NH0.FdIPpL_OpeZ2I-wcS4KlJiIspRD2Kd7aTz-Akn-dth_X8c7h4zki0Mt0DAjpDIhtBdf-bWXR0hw0wa5KO8iRuDqpqKTfhV6QcGOPsWArT59wcowxkoSyidSGrC103hBmbjWnxlJONnP5uZF_zZtA503DS6Yaefn8awX4E1RdoRg"
						},
						"column" : [
							{
								"value": "id",
								"type": "long"
							},
							{
								"value": "caseName",
								"type": "string"
							}
						]
					}
				},
				"writer": {
					"name": "mysqlwriter",
					"parameter": {
						"writeMode": "insert",
						"username": "root",
						"password": "root",
						"column": [
							"type",
							"name"
						],
						"connection": [
							{
								"jdbcUrl": "jdbc:mysql://127.0.0.1:3306/datax?characterEncoding=utf-8",
								"table": [
									"test"
								]
							}
						]
					}
				}
			}
		]
	}
}

```


### 3.2 参数说明

* **url**

  * 描述：rest请求，支持http，https协议 <br />

  * 必选：是 <br />

  * 默认值：无 <br />

* **method**

  * 描述：请求方法 <br />

  * 必选：是 get或post <br />

  * 默认值：无 <br />

* **timeout**

  * 描述：超时时间，单位秒 <br />

  * 必选：否 <br />

  * 默认值：10 <br />

* **parameters**

  * 描述：如timeRangeType=0&doorId=，不需要带问号。 <br />

  * 必选：否 <br />

  * 默认值：无 <br />
* **body**

  * 描述：post请求时使用，字符串。见示例<br />

  * 必选：否 <br />

  * 默认值：无<br />

* **dataPath**

  * 描述：指向json响应数组的数据路径，如 result.content，就代表着result属性下级的content属性，content属性是一个数组<br />

  * 必选：否 <br />

  * 默认值："" <br />

* **pagination**

  * 描述：是否分页的标志。 <br />

  * 必选：否。true使用分页，false不使用分页 <br />

  * 默认值：false。 <br />

* **totalParamPath**

  * 描述：json响应指向总记录数的变量，和dataPath结合访问，如值为total，代表着result.content下的total变量是总记录数<br />

  * 必选：是 <br />

  * 默认值：无 <br />

* **startIndexParam**

  * 描述：get请求中的url或者或者post请求的body中代表分页起始页的变量<br />

  * 必选：否 <br />

  * 默认值：pageIndex <br />

* **pageSizeParam**

  * 描述：get请求中的url或者或者post请求的body中代表分页页记录数的变量<br />

  * 必选：否 <br />

  * 默认值：pageSize <br />

* **pageNoParam**

  * 描述：get请求中的url或者或者post请求的body中代表分页页码的变量<br />

  * 必选：否 <br />

  * 默认值：pageNo <br />

* **customHeader**

  * 描述：自定义header值，是一个字典类型<br />

  * 必选：否 <br />

  * 默认值：无 <br />

* **column**

  * 描述：读取字段列表，type指定源数据的类型，index指定当前列来自于文本第几列(以0开始)，value指定当前类型为常量，不从源头文件读取数据，而是根据value值自动生成对应的列。 <br />

    用户可以指定Column字段信息，配置如下：

      ```json
           {
         "type": "long"
      },
      {
         "type": "string",
         "value": "alibaba"  
      }
      ```

    type目前只支持string，bool，long这些类型。

  * 必选：是 <br />

  * 默认值：全部按照string类型读取 <br />


## 4 性能报告



## 5 约束限制

略

## 6 FAQ

略

微信：18106218216 QQ：38775033 欢迎批评指正

