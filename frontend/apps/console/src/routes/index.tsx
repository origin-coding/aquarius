import { createFileRoute } from "@tanstack/react-router";
import { Button } from "antd";

export const Route = createFileRoute("/")({
  component: () => <Button>Hello</Button>,
});

