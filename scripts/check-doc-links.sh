#!/bin/bash

# EVCS文档链接检查脚本
# 检查所有markdown文档中的链接有效性

echo "🔍 开始检查文档链接..."

# 统计变量
total_files=0
total_links=0
broken_links=0

# 查找所有markdown文件（排除归档目录）
echo "📁 扫描文档文件..."
mapfile -t doc_files < <(find . -name "*.md" -not -path "./.git/*" -not -path "./docs/archive/*" -not -path "./build/*" -not -path "./node_modules/*")

echo "📄 找到 ${#doc_files[@]} 个文档文件"

# 检查每个文件
for file in "${doc_files[@]}"; do
    echo "  检查: $file"
    ((total_files++))

    # 提取所有链接
    while IFS= read -r line; do
        if [[ $line =~ \[.*\]\(([^)]+)\) ]]; then
            target="${BASH_REMATCH[1]}"
            ((total_links++))

            # 跳过锚点链接和邮件链接
            if [[ $target == \#* ]] || [[ $target == mailto:* ]]; then
                continue
            fi

            # 检查外部链接
            if [[ $target == http* ]]; then
                echo "    🌐 检查外部链接: $target"
                if ! curl -s --head --max-time 10 "$target" | grep -E "HTTP/.* [23].." > /dev/null 2>&1; then
                    echo "    ❌ 外部链接失效: $target"
                    ((broken_links++))
                fi
            # 检查内部链接
            elif [[ $target == /* ]]; then
                full_path=".${target}"
                if [[ ! -f "$full_path" ]]; then
                    echo "    ❌ 内部链接失效: $target (文件不存在: $full_path)"
                    ((broken_links++))
                fi
            else
                # 相对路径链接
                dir_path=$(dirname "$file")
                full_path="$dir_path/$target"
                if [[ ! -f "$full_path" ]]; then
                    echo "    ❌ 内部链接失效: $target (文件不存在: $full_path)"
                    ((broken_links++))
                fi
            fi
        fi
    done < <(grep -o '\[.*\]([^)]*)' "$file")
done

echo ""
echo "📊 链接检查完成"
echo "  📁 检查文件数: $total_files"
echo "  🔗 检查链接数: $total_links"
echo "  ❌ 失效链接数: $broken_links"

if [ $broken_links -eq 0 ]; then
    echo "  ✅ 所有链接正常"
    exit 0
else
    echo "  🚨 发现失效链接，请及时修复"
    exit 1
fi