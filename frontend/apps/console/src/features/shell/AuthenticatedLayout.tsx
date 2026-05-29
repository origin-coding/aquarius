import {
  AppstoreOutlined,
  BellOutlined,
  LogoutOutlined,
  SettingOutlined,
  SmileOutlined,
  TeamOutlined,
} from "@ant-design/icons";
import {ProLayout} from "@ant-design/pro-components";
import {Link, Outlet, useRouterState} from "@tanstack/react-router";
import {Button, Dropdown} from "antd";
import {useState} from "react";

import {useLogoutMutation} from "@/features/auth/authMutations";
import {useAuthStore} from "@/features/auth/authStore";

const appRoute = {
  path: "/",
  children: [
    {
      path: "/dashboard",
      name: "工作台",
      icon: <AppstoreOutlined />,
    },
    {
      path: "/iam",
      name: "身份与权限",
      icon: <TeamOutlined />,
      disabled: true,
    },
    {
      path: "/system",
      name: "系统设置",
      icon: <SettingOutlined />,
      disabled: true,
    },
  ],
};

const headerActions = [
  {
    key: "notifications",
    icon: <BellOutlined />,
  },
];

const brandLogo = (
  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[#1677ff] font-semibold text-white shadow-sm shadow-blue-200">
    A
  </div>
);

export function AuthenticatedLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const logoutMutation = useLogoutMutation();
  const user = useAuthStore((state) => state.user);
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const displayName = user?.name ?? user?.username ?? "Admin";

  return (
    <ProLayout
      actionsRender={() =>
        headerActions.map((action) => <Button icon={action.icon} key={action.key} type="text" />)
      }
      avatarProps={{
        icon: <SmileOutlined />,
        size: 34,
        title: displayName,
        render: (_, defaultDom) => (
          <Dropdown
            menu={{
              onClick: ({key}) => {
                if (key === "logout") {
                  logoutMutation.mutate();
                }
              },
              items: [
                {
                  key: "profile",
                  label: "个人设置",
                  disabled: true,
                },
                {
                  type: "divider",
                },
                {
                  key: "logout",
                  icon: <LogoutOutlined />,
                  label: "退出登录",
                  disabled: logoutMutation.isPending,
                },
              ],
            }}
            placement="bottomRight"
          >
            {defaultDom}
          </Dropdown>
        ),
      }}
      breakpoint={false}
      collapsed={collapsed}
      contentStyle={{
        paddingBlock: 0,
        paddingInline: 0,
      }}
      fixedHeader
      fixSiderbar
      headerTitleRender={(logo, title) => (
        <Link className="flex items-center gap-3 text-slate-950" to="/dashboard">
          {logo}
          <span className="text-lg leading-none font-semibold">{title}</span>
        </Link>
      )}
      layout="mix"
      locale="zh-CN"
      location={{ pathname }}
      logo={brandLogo}
      menu={{
        collapsedWidth: 72,
        locale: false,
      }}
      menuHeaderRender={false}
      menuItemRender={(item, defaultDom) => {
        if (item.disabled || !item.path) {
          return defaultDom;
        }

        return <Link to={item.path}>{defaultDom}</Link>;
      }}
      onCollapse={setCollapsed}
      pageTitleRender={false}
      route={appRoute}
      selectedKeys={[pathname]}
      siderWidth={248}
      splitMenus={false}
      title="Aquarius Console"
      token={{
        bgLayout: "#f5f5f5",
        header: {
          colorBgHeader: "#ffffff",
          heightLayoutHeader: 64,
        },
        sider: {
          colorBgMenuItemHover: "rgba(22, 119, 255, 0.06)",
          colorBgMenuItemSelected: "rgba(22, 119, 255, 0.10)",
          colorMenuBackground: "#f5f5f5",
          colorTextMenuSelected: "#1677ff",
        },
      }}
    >
      <Outlet />
    </ProLayout>
  );
}
