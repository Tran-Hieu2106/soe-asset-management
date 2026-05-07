import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { Title } = Typography;

// Define interface for the payload
interface LoginFormData {
  username: string;
  password: string;
}

// Define interface for API Response based on api-spec.md
interface LoginResponse {
  success: boolean;
  message: string;
  data?: {
    token: string;
    username: string;
  };
}

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const onFinish = async (values: LoginFormData) => {
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(values),
      });

      const result: LoginResponse = await response.json();

      if (response.ok && result.success && result.data) {
        message.success(result.message || 'Login successful!');
        
        // Store token in localStorage for subsequent requests
        localStorage.setItem('jwt_token', result.data.token);
        localStorage.setItem('current_username', result.data.username);
        
        // Redirect user to dashboard/home
        navigate('/'); 
      } else {
        message.error(result.message || 'Invalid username or password.');
      }
    } catch (error) {
      console.error('Login error:', error);
      message.error('Cannot connect to the server. Please check the backend.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100vh', 
      backgroundColor: '#f0f2f5' 
    }}>
      <Card 
        style={{ width: 400, boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
        bodyStyle={{ padding: '32px' }}
      >
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Title level={3} style={{ margin: 0 }}>SOE Asset Management</Title>
          <div style={{ color: '#8c8c8c', marginTop: 8 }}>
            Asset Management System Login
          </div>
        </div>

        <Form
          name="login_form"
          layout="vertical"
          onFinish={onFinish}
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: 'Please input your username!' }]}
          >
            <Input 
              prefix={<UserOutlined style={{ color: '#bfbfbf' }} />} 
              placeholder="Username (e.g., admin)" 
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: 'Please input your password!' }]}
          >
            <Input.Password 
              prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} 
              placeholder="Password (e.g., Password@123)" 
            />
          </Form.Item>

          <Form.Item style={{ marginTop: 24, marginBottom: 0 }}>
            <Button 
              type="primary" 
              htmlType="submit" 
              loading={loading} 
              block
            >
              Log in
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Login;