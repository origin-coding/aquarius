import {
  AppstoreOutlined,
  BellOutlined,
  GlobalOutlined,
  LogoutOutlined,
  SettingOutlined,
  SmileOutlined,
  TeamOutlined,
} from "@ant-design/icons";
import { ProLayout } from "@ant-design/pro-components";
import { Link, Outlet, useRouterState } from "@tanstack/react-router";
import { Button, Dropdown } from "antd";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { useLogoutMutation } from "@/features/auth/authMutations";
import { useAuthStore } from "@/features/auth/authStore";
import type { SupportedLocale } from "@/i18n";
import { changeLocale, getCurrentLocale } from "@/i18n/locale";

const brandLogo = (
  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[#1677ff] font-semibold text-white shadow-sm shadow-blue-200">
    A
  </div>
);

export function AuthenticatedLayout() {
  const { t } = useTranslation();
  const { t: tMenu } = useTranslation("menu");
  const [collapsed, setCollapsed] = useState(false);
  const logoutMutation = useLogoutMutation();
  const user = useAuthStore((state) => state.user);
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const displayName = user?.name ?? user?.username ?? "Admin";
  const currentLocale = getCurrentLocale();
  const appRoute = {
    path: "/",
    children: [
      {
        path: "/dashboard",
        name: tMenu(($) => $.dashboard),
        icon: <AppstoreOutlined />,
      },
      {
        path: "/iam",
        name: tMenu(($) => $.iam),
        icon: <TeamOutlined />,
        disabled: true,
      },
      {
        path: "/system",
        name: tMenu(($) => $.system),
        icon: <SettingOutlined />,
        disabled: true,
      },
    ],
  };

  const handleLocaleChange = (locale: SupportedLocale) => {
    if (locale !== currentLocale) {
      void changeLocale(locale);
    }
  };

  return (
    <ProLayout
      actionsRender={() => [
        <Button
          aria-label={tMenu(($) => $.notifications)}
          icon={<BellOutlined />}
          key="notifications"
          type="text"
        />,
        <Dropdown
          key="language"
          menu={{
            onClick: ({ key }) => handleLocaleChange(key as SupportedLocale),
            selectedKeys: [currentLocale],
            items: [
              {
                key: "zh-CN",
                label: t(($) => $.language.zhCN),
              },
              {
                key: "en-US",
                label: t(($) => $.language.enUS),
              },
            ],
          }}
          placement="bottomRight"
          trigger={["click"]}
        >
          <Button icon={<GlobalOutlined />} type="text">
            {currentLocale}
          </Button>
        </Dropdown>,
      ]}
      avatarProps={{
        icon: <SmileOutlined />,
        size: 34,
        title: displayName,
        render: (_, defaultDom) => (
          <Dropdown
            menu={{
              onClick: ({ key }) => {
                if (key === "logout") {
                  logoutMutation.mutate();
                }
              },
              items: [
                {
                  key: "profile",
                  label: tMenu(($) => $.profile),
                  disabled: true,
                },
                {
                  type: "divider",
                },
                {
                  key: "logout",
                  icon: <LogoutOutlined />,
                  label: tMenu(($) => $.logout),
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
      locale={currentLocale}
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
      title={t(($) => $.app.console)}
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

