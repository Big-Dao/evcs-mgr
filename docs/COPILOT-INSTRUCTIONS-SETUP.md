# GitHub Copilot Instructions Setup

**Date**: 2025-10-14  
**Status**: ✅ Completed  
**Issue**: [Setup Copilot instructions](https://github.com/Big-Dao/evcs-mgr/issues/XXX)

## Overview

This document describes the GitHub Copilot instructions setup for the EVCS Manager project, following [GitHub's best practices](https://docs.github.com/en/copilot/tutorials/coding-agent/get-the-best-results).

## What Was Implemented

### 1. Main Copilot Instructions (`.github/copilot-instructions.md`)

**Size**: 292 lines, 9.7KB  
**Scope**: Repository-wide guidance for all code

**Sections Added**:
- **Project Overview**: Context about the EVCS platform, tech stack, and links to key documentation
- **Core Architecture**: Detailed explanation of the four-layer multi-tenant data isolation system
- **Development Patterns**: Code examples for services and controllers
- **Build & Testing Commands**: Comprehensive Gradle commands for building, testing, and running services
- **Code Quality Standards**: Naming conventions, code organization, and testing best practices
- **Common Pitfalls**: List of frequent mistakes and how to avoid them
- **Git Workflow**: Commit message format (Conventional Commits) and branch naming

**Key Features**:
- Links to main documentation (`README.md`, `DEVELOPER-GUIDE.md`, etc.)
- Real code examples from the project
- Multi-tenant pattern emphasis throughout
- Testing patterns with `TenantContext` management

### 2. Path-Specific Instructions (`.github/instructions/`)

Created targeted guidance for specific modules and code types:

#### a) Station Module (`station.instructions.md`)
**Size**: 60 lines, 2.2KB  
**Scope**: `evcs-station/**/*.java`

**Content**:
- Multi-tenant isolation requirements
- Station hierarchy handling
- Real-time update patterns
- Offline detection logic
- Database schema specifics

#### b) Common Module (`common.instructions.md`)
**Size**: 61 lines, 1.9KB  
**Scope**: `evcs-common/**/*.java`

**Content**:
- Backward compatibility requirements
- Tenant framework implementation details
- Zero business logic policy
- Threading and context management

#### c) Test Code (`test.instructions.md`)
**Size**: 121 lines, 3.1KB  
**Scope**: `**/src/test/**/*.java`

**Content**:
- AAA pattern enforcement
- Test naming conventions
- Base test class usage
- Tenant context management in tests
- Test data generation with `TestDataFactory`
- Multi-tenant isolation testing patterns
- Performance testing guidelines

### 3. GitHub Directory Documentation (`.github/README.md`)

**Size**: 48 lines, 1.5KB

**Content**:
- Overview of all `.github` files
- Explanation of Copilot instructions setup
- Future enhancement possibilities
- Links to GitHub documentation

## Total Impact

- **5 files created/enhanced**
- **582 lines of guidance**
- **18.7KB of documentation**
- **Repository-wide + 3 path-specific instruction sets**

## How Copilot Will Use These Instructions

1. **General Coding**: Uses `.github/copilot-instructions.md` for all code suggestions
2. **Station Module**: Combines main instructions + `station.instructions.md` when editing station code
3. **Common Module**: Combines main instructions + `common.instructions.md` for framework code
4. **Test Code**: Combines main instructions + `test.instructions.md` for test files

## Benefits

### For Development
- ✅ **Better Context**: Copilot understands multi-tenant architecture
- ✅ **Consistent Patterns**: Enforces established coding patterns
- ✅ **Tenant Isolation**: Emphasizes proper `TenantContext` usage
- ✅ **Test Quality**: Guides proper test structure and tenant isolation testing

### For New Contributors
- ✅ **Quick Onboarding**: Instructions provide immediate context
- ✅ **Best Practices**: Built-in guidance on project conventions
- ✅ **Examples**: Real code examples from the project

### For Code Quality
- ✅ **Standardization**: Promotes consistent code across modules
- ✅ **Error Prevention**: Highlights common pitfalls
- ✅ **Documentation**: Encourages proper documentation practices

## Verification

To verify the setup is working:

1. **Check File Existence**:
   ```bash
   ls -la .github/copilot-instructions.md
   ls -la .github/instructions/
   ```

2. **Test with Copilot**:
   - Open a file in `evcs-station/`
   - Ask Copilot to create a new service method
   - Verify it includes `@DataScope` and tenant context handling

3. **Test Path-Specific Instructions**:
   - Open a test file
   - Ask Copilot to create a new test
   - Verify it follows AAA pattern and includes `@DisplayName`

## Future Enhancements

### Additional Path-Specific Instructions
Consider adding instructions for:
- `payment.instructions.md` - Payment gateway integration patterns
- `protocol.instructions.md` - OCPP/CloudCharge protocol handling
- `order.instructions.md` - Order processing and billing logic

### Template Files
Could add instruction templates:
```
.github/instructions/
├── _template.instructions.md  # Template for new instructions
├── controller.instructions.md # Controller-specific patterns
└── mapper.instructions.md     # MyBatis mapper patterns
```

### Integration with CI/CD
- Add workflow to lint instruction files
- Validate YAML frontmatter in path-specific instructions
- Check for broken documentation links

## Maintenance

### When to Update

Update instructions when:
- Architecture patterns change
- New development practices are established
- Common mistakes are identified
- New modules are added
- Build/test processes change

### Review Schedule

Recommend reviewing every quarter:
- Are instructions still accurate?
- Are there new patterns to document?
- Are there outdated references?

## References

- [GitHub Copilot Custom Instructions Documentation](https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions)
- [Best Practices for GitHub Copilot](https://docs.github.com/en/copilot/tutorials/coding-agent/get-the-best-results)
- [Project Developer Guide](./DEVELOPER-GUIDE.md)
- [Project Testing Guide](./TESTING-QUICKSTART.md)

## Conclusion

The GitHub Copilot instructions setup is complete and follows industry best practices. The combination of repository-wide and path-specific instructions provides comprehensive guidance for the coding agent while maintaining focus on the project's critical multi-tenant architecture patterns.

---

**Setup completed by**: GitHub Copilot  
**Date**: 2025-10-14  
**Version**: 1.0
