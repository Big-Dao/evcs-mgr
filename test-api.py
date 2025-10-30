#!/usr/bin/env python3
"""测试新实现的后端API"""
import urllib.request
import urllib.parse
import json

BASE_URL = "http://localhost:8080"

def login():
    """登录获取token"""
    url = f"{BASE_URL}/auth/login"
    data = {
        "username": "admin",
        "password": "password",
        "tenantCode": "SYSTEM"
    }
    req = urllib.request.Request(url, 
                                  data=json.dumps(data).encode(),
                                  headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read().decode())
        if result["success"]:
            print(f"✓ 登录成功: {result['data']['user']['username']} (租户ID: {result['data']['user']['tenantId']})")
            return result["data"]["accessToken"]
        else:
            raise Exception(f"登录失败: {result['message']}")
    except urllib.error.HTTPError as e:
        error_body = e.read().decode()
        print(f"Login HTTP Error {e.code}: {error_body}")
        raise Exception(f"Login failed: HTTP {e.code}")

def test_role_list(token):
    """测试角色列表API"""
    print("\n=== 1. 角色列表API ===")
    url = f"{BASE_URL}/auth/role/list"
    print(f"请求URL: {url}")
    print(f"Token前30字符: {token[:30]}...")
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read().decode())
        if result["success"]:
            roles = result["data"]
            print(f"✓ 成功! 返回 {len(roles)} 个角色:")
            for role in roles:
                print(f"  - {role['roleName']} ({role['roleCode']}) - 状态: {'启用' if role['status'] == 1 else '禁用'}")
        else:
            print(f"✗ 失败: {result['message']}")
    except urllib.error.HTTPError as e:
        print(f"✗ HTTP错误 {e.code}: {e.reason}")
        print(f"响应内容: {e.read().decode()}")

def test_menu_list(token):
    """测试菜单列表API"""
    print("\n=== 2. 菜单列表API ===")
    url = f"{BASE_URL}/auth/menu/list"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode())
    if result["success"]:
        menus = result["data"]
        print(f"✓ 成功! 返回 {len(menus)} 个菜单项:")
        for menu in menus[:5]:  # 只显示前5个
            print(f"  - {menu['menuName']} (类型: {'目录' if menu['menuType'] == 0 else '菜单'}) - 可见: {'是' if menu['visible'] == 1 else '否'}")
        if len(menus) > 5:
            print(f"  ... 还有 {len(menus) - 5} 个菜单")
    else:
        print(f"✗ 失败: {result['message']}")

def test_dashboard_station_ranking(token):
    """测试站点排名API"""
    print("\n=== 3. 站点排名API ===")
    url = f"{BASE_URL}/dashboard/station-ranking"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode())
    if result["success"]:
        rankings = result["data"]
        print(f"✓ 成功! 返回 {len(rankings)} 个站点:")
        for rank in rankings:
            print(f"  - {rank['stationName']}: {rank['orderCount']} 订单 ({rank['percentage']}%)")
    else:
        print(f"✗ 失败: {result['message']}")

def test_dashboard_charger_utilization(token):
    """测试充电桩利用率API"""
    print("\n=== 4. 充电桩利用率API ===")
    url = f"{BASE_URL}/dashboard/charger-utilization"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode())
    if result["success"]:
        chargers = result["data"]
        print(f"✓ 成功! 返回 {len(chargers)} 个充电桩:")
        for charger in chargers:
            print(f"  - {charger['chargerCode']} ({charger['stationName']}): 利用率 {charger['utilizationRate']}%")
    else:
        print(f"✗ 失败: {result['message']}")

if __name__ == "__main__":
    try:
        # 登录
        token = login()
        
        # 测试所有API
        test_role_list(token)
        test_menu_list(token)
        test_dashboard_station_ranking(token)
        test_dashboard_charger_utilization(token)
        
        print("\n" + "="*50)
        print("✓ 所有API测试完成!")
        
    except Exception as e:
        print(f"\n✗ 测试失败: {e}")
        import traceback
        traceback.print_exc()
