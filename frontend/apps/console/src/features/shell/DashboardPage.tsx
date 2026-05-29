import {
  InfoCircleOutlined,
  SettingOutlined,
} from "@ant-design/icons";
import { PageContainer } from "@ant-design/pro-components";
import { Button, Card, Progress, Segmented, Space, Typography } from "antd";
import type { ReactNode } from "react";

const { Text, Title } = Typography;

const monthlyValues = [680, 590, 1100, 780, 920, 740, 260, 430, 1120, 660, 870, 930];
const paymentValues = [68, 46, 38, 18, 42, 68, 52, 61, 55, 84, 60, 35, 5, 50, 41, 63, 54];
const visitTrend = [48, 40, 36, 54, 45, 50, 46, 72, 52, 34, 22, 43, 32, 51, 47];

const rankingItems = [
  "IAM 登录认证",
  "本地验证码发放",
  "访问令牌刷新",
  "会话注销",
  "权限校验",
  "审计事件",
  "系统配置",
];

export function DashboardPage() {
  return (
    <PageContainer
      content="身份认证与系统运行概览"
      extra={<Button type="primary">新建</Button>}
      title="分析页"
    >
      <main className="mx-auto max-w-[1440px]">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <MetricCard
            footer="日登录量 1,234"
            title="登录次数"
            trend={<TrendArea values={visitTrend} />}
            value="8,846"
          />
          <MetricCard
            footer="活跃账号 256"
            title="活跃用户"
            trend={<MiniBars values={paymentValues} />}
            value="6,560"
          />
          <MetricCard
            footer="同比 12% ▲　环比 11% ▼"
            title="认证成功率"
            trend={<Progress percent={98} showInfo={false} strokeColor="#52c41a" />}
            value="98%"
          />
          <MetricCard
            footer="异常请求 3"
            title="安全事件"
            trend={<Progress percent={78} showInfo={false} strokeColor="#1677ff" />}
            value="78%"
          />
        </div>

        <Card
          className="!mt-5"
          classNames={{ body: "!p-0" }}
          title={
            <div className="flex items-center justify-between">
              <Segmented options={["登录量", "访问量"]} value="登录量" />
              <Space size={18}>
                <Button type="link">今日</Button>
                <Button type="link">本周</Button>
                <Button type="link">本月</Button>
                <Button type="link">本年</Button>
              </Space>
            </div>
          }
        >
          <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_340px]">
            <div className="px-7 py-8">
              <BarChart values={monthlyValues} />
            </div>
            <div className="border-t border-slate-100 px-7 py-8 xl:border-l xl:border-t-0">
              <Title level={5} className="!mb-5">
                模块调用排名
              </Title>
              <Space className="w-full" orientation="vertical" size={14}>
                {rankingItems.map((item, index) => (
                  <div className="flex items-center justify-between" key={item}>
                    <Space>
                      <span
                        className={`h-6 w-6 rounded-full flex items-center justify-center text-xs ${
                          index < 3
                            ? "bg-slate-900 text-white"
                            : "bg-slate-100 text-slate-500"
                        }`}
                      >
                        {index + 1}
                      </span>
                      <Text>{item}</Text>
                    </Space>
                    <Text className="text-slate-500">
                      {(323234 - index * 8421).toLocaleString()}
                    </Text>
                  </div>
                ))}
              </Space>
            </div>
          </div>
        </Card>

        <div className="mt-5 grid grid-cols-1 gap-4 xl:grid-cols-2">
          <Card
            extra={<Button type="text">...</Button>}
            title="线上热门搜索"
          >
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Text className="text-slate-500">
                  搜索用户数 <InfoCircleOutlined />
                </Text>
                <Title level={3} className="!mb-0 !mt-2">
                  12,321
                </Title>
              </div>
              <div>
                <Text className="text-slate-500">
                  人均搜索次数 <InfoCircleOutlined />
                </Text>
                <Title level={3} className="!mb-0 !mt-2">
                  2.7
                </Title>
              </div>
            </div>
          </Card>

          <Card
            extra={<Segmented options={["全部渠道", "线上", "门店"]} value="全部渠道" />}
            title="认证类型占比"
          >
            <div className="grid grid-cols-[140px_minmax(0,1fr)] items-center gap-6">
              <div className="h-32 w-32 rounded-full border-[18px] border-blue-500 border-r-cyan-400 border-b-emerald-400" />
              <Space orientation="vertical" size={12}>
                <div>
                  <Text className="block font-medium">密码登录</Text>
                  <Progress percent={72} showInfo={false} />
                </div>
                <div>
                  <Text className="block font-medium">刷新令牌</Text>
                  <Progress percent={46} showInfo={false} strokeColor="#13c2c2" />
                </div>
                <div>
                  <Text className="block font-medium">验证码挑战</Text>
                  <Progress percent={28} showInfo={false} strokeColor="#52c41a" />
                </div>
              </Space>
            </div>
          </Card>
        </div>

        <Button
          className="!fixed !right-0 !top-[270px] !z-20 !h-14 !w-14 !rounded-r-none"
          icon={<SettingOutlined />}
          size="large"
          type="primary"
        />
      </main>
    </PageContainer>
  );
}

type MetricCardProps = {
  footer: string;
  title: string;
  trend: ReactNode;
  value: string;
};

function MetricCard({ footer, title, trend, value }: MetricCardProps) {
  return (
    <Card className="shadow-sm shadow-slate-200/60">
      <div className="mb-6 flex items-center justify-between">
        <Title level={4} className="!mb-0 !text-[18px]">
          {title}
        </Title>
        <InfoCircleOutlined className="text-slate-400" />
      </div>
      <Title level={2} className="!mb-4 !text-[34px] !font-normal">
        {value}
      </Title>
      <div className="h-14">{trend}</div>
      <div className="mt-4 border-t border-slate-100 pt-3">
        <Text className="text-slate-600">{footer}</Text>
      </div>
    </Card>
  );
}

function TrendArea({ values }: { values: number[] }) {
  const max = Math.max(...values);
  const points = values
    .map((value, index) => {
      const x = (index / (values.length - 1)) * 100;
      const y = 52 - (value / max) * 44;
      return `${x},${y}`;
    })
    .join(" ");

  return (
    <svg className="h-full w-full" preserveAspectRatio="none" viewBox="0 0 100 56">
      <polygon fill="rgba(114, 46, 209, 0.16)" points={`0,56 ${points} 100,56`} />
      <polyline fill="none" points={points} stroke="#b37feb" strokeWidth="2" />
    </svg>
  );
}

function MiniBars({ values }: { values: number[] }) {
  const max = Math.max(...values);

  return (
    <div className="flex h-full items-end gap-2">
      {values.map((value, index) => (
        <div
          className="w-full rounded-t-sm bg-[#1677ff]"
          key={`${value}-${index}`}
          style={{ height: `${Math.max(8, (value / max) * 52)}px` }}
        />
      ))}
    </div>
  );
}

function BarChart({ values }: { values: number[] }) {
  const max = Math.max(...values);

  return (
    <div className="flex h-[320px] items-end gap-5 border-b border-slate-200 pl-4">
      {values.map((value, index) => (
        <div className="flex flex-1 flex-col items-center gap-3" key={`${value}-${index}`}>
          <div
            className="w-full max-w-[52px] rounded-t-sm bg-[#1677ff]"
            style={{ height: `${Math.max(24, (value / max) * 285)}px` }}
          />
          <Text className="text-slate-400">{index + 1}月</Text>
        </div>
      ))}
    </div>
  );
}
