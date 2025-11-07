#!/bin/bash

# EVCS文档格式检查脚本
# 检查文档格式规范和完整性

echo "📝 开始检查文档格式..."

# 统计变量
total_files=0
issues=0

# 查找所有markdown文件
mapfile -t doc_files < <(find . -name "*.md" -not -path "./.git/*" -not -path "./docs/archive/*" -not -path "./build/*" -not -path "./node_modules/*")

echo "📄 找到 ${#doc_files[@]} 个文档文件"

for file in "${doc_files[@]}"; do
    echo "  检查: $file"
    ((total_files++))
    file_issues=0

    # 检查是否有标题
    if ! grep -q "^# " "$file"; then
        echo "    ❌ 缺少主标题"
        ((file_issues++))
    fi

    # 检查是否有版本信息
    if ! grep -q -i "版本\|version" "$file"; then
        echo "    ⚠️ 缺少版本信息"
        ((file_issues++))
    fi

    # 检查是否有更新日期
    if ! grep -q -i "更新日期\|update.*date\|date.*update" "$file"; then
        echo "    ⚠️ 缺少更新日期"
        ((file_issues++))
    fi

    # 检查代码块格式
    if grep -q '```$' "$file"; then
        echo "    ⚠️ 存在未指定语言的代码块"
        ((file_issues++))
    fi

    # 检查是否有过长的行（建议不超过120字符）
    while IFS= read -r line; do
        if [ ${#line} -gt 120 ] && [[ $line != http* ]]; then
            echo "    ⚠️ 存在过长行 (${#line}字符): ${line:0:50}..."
            ((file_issues++))
            break
        fi
    done < "$file"

    # 检查是否包含中文但未指定UTF-8编码
    if grep -q "[\u4e00-\u9fff]" "$file"; then
        if ! grep -q -i "utf-8\|utf8" "$file"; then
            echo "    ⚠️ 包含中文但未指定UTF-8编码"
            ((file_issues++))
        fi
    fi

    # 检查是否有相关文档章节
    if [[ "$file" == docs/**/*.md ]] && ! grep -q -i "相关文档\|related.*doc\|reference" "$file"; then
        echo "    ⚠️ 建议添加相关文档章节"
        ((file_issues++))
    fi

    # 统计文件问题数
    if [ $file_issues -gt 0 ]; then
        ((issues += file_issues))
        echo "    📊 发现 $file_issues 个问题"
    else
        echo "    ✅ 格式检查通过"
    fi
done

echo ""
echo "📊 格式检查完成"
echo "  📁 检查文件数: $total_files"
echo "  ⚠️ 发现问题数: $issues"

if [ $issues -eq 0 ]; then
    echo "  ✅ 所有文档格式正确"
    exit 0
else
    echo "  🚨 发现格式问题，请根据建议进行修复"
    exit 1
fi