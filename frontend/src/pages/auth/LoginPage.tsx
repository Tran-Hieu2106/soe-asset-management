import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Alert } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { authApi } from '../../api/authApi';
import { useAuthStore } from '../../store/authStore';
import './LoginPage.css';

// ── Types ─────────────────────────────────────────────────────

interface LoginFormValues {
  username: string;
  password: string;
}

// ── Component ─────────────────────────────────────────────────

export default function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form] = Form.useForm<LoginFormValues>();

  const handleSubmit = async (values: LoginFormValues) => {
    setLoading(true);
    setError(null);

    try {
      // Step 1 — login, get token
      const loginData = await authApi.login(values);

      // Step 2 — fetch full user profile (roles, units etc.)
      // We need the token stored first so axiosInstance can attach it
      // Use a temporary store set before getMe()
      useAuthStore.setState({ token: loginData.token, user: null });

      const user = await authApi.getMe();

      // Step 3 — store both token + user
      setAuth(loginData.token, user);

      // Step 4 — redirect to dashboard
      navigate('/dashboard', { replace: true });

    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })
          ?.response?.data?.message;

      if (msg?.toLowerCase().includes('bad credentials') ||
          msg?.toLowerCase().includes('credentials')) {
        setError('Tên đăng nhập hoặc mật khẩu không đúng.');
      } else {
        setError('Đăng nhập thất bại. Vui lòng thử lại sau.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">

        {/* Header */}
        <div className="login-header">
          <div className="login-emblem">
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="48" height="48" rx="10" fill="var(--login-accent)"/>
              <path d="M24 10L28 18H36L30 23L32 32L24 27L16 32L18 23L12 18H20L24 10Z"
                    fill="white" opacity="0.95"/>
            </svg>
          </div>
          <h1 className="login-title">HỆ THỐNG QUẢN LÝ TÀI SẢN <br /> DOANH NGHIỆP NHÀ NƯỚC</h1>
        </div>

        {/* Error alert */}
        {error && (
          <Alert
            title={error}
            type="error"
            showIcon
            closable
            onClose={() => setError(null)}
            style={{ marginBottom: 20 }}
          />
        )}

        {/* Form */}
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            label="Tên đăng nhập"
            rules={[
              { required: true, message: 'Vui lòng nhập tên đăng nhập.' },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="Nhập tên đăng nhập"
              autoFocus
            />
          </Form.Item>

          <Form.Item
            name="password"
            label="Mật khẩu"
            rules={[
              { required: true, message: 'Vui lòng nhập mật khẩu.' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Nhập mật khẩu"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, marginTop: 8 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              className="login-btn"
            >
              {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
            </Button>
          </Form.Item>
        </Form>

        {/* Footer */}
        <p className="login-footer">
          Phiên bản 1.0 &nbsp;·&nbsp; SE Qua Tao &nbsp;·&nbsp; HUST SoICT
        </p>
      </div>
    </div>
  );
}
