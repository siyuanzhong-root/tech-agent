package com.edu.agent.service;

import com.edu.agent.model.Grade;
import com.edu.agent.model.Student;
import com.edu.agent.repository.GradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EduAgentService {

    private static final Logger logger = LoggerFactory.getLogger(EduAgentService.class);

    @Autowired
    private GradeService gradeService;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private DeepSeekService deepSeekService;

    public String processRequest(String userId, String request) {
        logger.info("[EduAgent] 收到用户请求: {}", request);

        // 记录用户的请求类型，用于用户画像分析
        updateUserPreference(userId, request);

        // 获取用户上下文
        Context context = getContext(userId);
        logger.info("[EduAgent] 当前上下文: {}", context);

        // 每个请求都调用DeepSeek进行智能意图识别
        String intent = recognizeIntentWithDeepSeek(request);
        logger.info("[EduAgent] DeepSeek识别的意图: {}", intent);

        // 根据意图执行相应的操作
        switch (intent) {
            case "sort":
                return handleSortRequestWithDeepSeek(request, context, userId);
            case "add":
                return handleAddGradeWithDeepSeek(request, context, userId);
            case "update":
                return handleUpdateGradeWithDeepSeek(request, context, userId);
            case "delete":
                return handleDeleteGradeWithDeepSeek(request, context, userId);
            case "query":
                return handleQueryWithDeepSeek(request, context, userId);
            case "analysis":
                return handleAnalysisWithDeepSeek(request, context, userId);
            default:
                // 使用DeepSeek处理其他类型的请求
                return handleAIGenericRequest(request, userId);
        }
    }

    private String recognizeIntentWithDeepSeek(String request) {
        // 每个请求都调用DeepSeek进行智能意图识别
        logger.info("[EduAgent] 调用DeepSeek进行意图识别...");

        String systemPrompt = "你是一个意图识别助手。请分析用户的输入，判断用户的意图是什么。\n" +
                "可选的意图类型：\n" +
                "- sort: 成绩排序、排名相关\n" +
                "- add: 添加、录入成绩相关\n" +
                "- update: 修改、更新成绩相关（如：把XX改成XX、修改为、更新为等）\n" +
                "- delete: 删除、移除成绩相关\n" +
                "- query: 查询、查看成绩相关\n" +
                "- analysis: 分析、统计成绩相关\n" +
                "- other: 其他类型的问题\n\n" +
                "请只返回意图类型（sort/add/update/delete/query/analysis/other），不要返回其他内容。";

        String aiResponse = deepSeekService.chatWithSystemPrompt(systemPrompt, request);

        // 解析AI返回的意图
        String intent = aiResponse.trim().toLowerCase();
        if (intent.contains("sort")) return "sort";
        if (intent.contains("add")) return "add";
        if (intent.contains("update")) return "update";
        if (intent.contains("delete")) return "delete";
        if (intent.contains("query")) return "query";
        if (intent.contains("analysis")) return "analysis";

        return "other";
    }

    private ExtractedInfo extractInfoWithDeepSeek(String request, String actionType, Context context) {
        logger.info("[EduAgent] 调用DeepSeek提取{}信息...", actionType);

        // 构建包含上下文的提示
        String contextPrompt = "";
        if (context.studentName != null) {
            contextPrompt += "用户之前查询过学生: " + context.studentName + "\n";
        }
        if (context.subject != null) {
            contextPrompt += "用户之前查询过科目: " + context.subject + "\n";
        }
        if (context.semester != null) {
            contextPrompt += "用户之前查询过学期: " + context.semester + "\n";
        }
        if (!contextPrompt.isEmpty()) {
            contextPrompt = "上下文信息（如果用户没有明确指定，请使用这些信息）：\n" + contextPrompt + "\n";
        }

        String systemPrompt = "你是一个教务信息提取助手。" + contextPrompt +
                "请从用户的输入中提取以下信息，并以JSON格式返回：\n" +
                "{\n" +
                "  \"studentId\": \"学生ID（数字，如果有）\",\n" +
                "  \"studentName\": \"学生姓名（如果有）\",\n" +
                "  \"subject\": \"科目（数学、语文、英语、物理、化学、生物、历史、地理、政治之一）\",\n" +
                "  \"score\": \"分数（数字，如果有）\",\n" +
                "  \"newScore\": \"新分数（数字，修改时使用）\",\n" +
                "  \"semester\": \"学期（如：2024年上学期，可选）\",\n" +
                "  \"sortOrder\": \"排序方向（asc-升序/desc-降序，排序时使用）\",\n" +
                "  \"queryType\": \"查询类型（all-所有成绩/subject-按科目/semester-按学期/student-按学生）\"\n" +
                "}\n" +
                "如果某项信息缺失，请使用null。只返回JSON，不要其他说明。";

        String aiResponse = deepSeekService.chatWithSystemPrompt(systemPrompt, request);
        logger.info("[EduAgent] DeepSeek提取的信息: {}", aiResponse);

        return parseExtractedInfo(aiResponse);
    }

    private ExtractedInfo parseExtractedInfo(String json) {
        ExtractedInfo info = new ExtractedInfo();

        // 简单的JSON解析
        String jsonStr = json.replaceAll("(?s).*\\{", "{").replaceAll("}.*", "}");

        info.studentId = extractJsonValue(jsonStr, "studentId");
        info.studentName = extractJsonValue(jsonStr, "studentName");
        info.subject = extractJsonValue(jsonStr, "subject");
        info.score = parseDouble(extractJsonValue(jsonStr, "score"));
        info.newScore = parseDouble(extractJsonValue(jsonStr, "newScore"));
        info.semester = extractJsonValue(jsonStr, "semester");
        info.sortOrder = extractJsonValue(jsonStr, "sortOrder");
        info.queryType = extractJsonValue(jsonStr, "queryType");

        return info;
    }

    private Double parseDouble(String value) {
        if (value == null || value.equals("null")) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String handleSortRequestWithDeepSeek(String request, Context context, String userId) {
        ExtractedInfo info = extractInfoWithDeepSeek(request, "排序", context);

        // 如果没有指定学生，使用上下文中的学生
        if (info.studentName == null && info.studentId == null) {
            info.studentName = context.studentName;
            info.studentId = context.studentId;
        }
        // 如果没有指定科目，使用上下文中的科目
        if (info.subject == null) {
            info.subject = context.subject;
        }
        // 如果没有指定学期，使用上下文中的学期
        if (info.semester == null) {
            info.semester = context.semester;
        }

        List<Grade> grades;
        if (info.studentName != null || info.studentId != null) {
            // 按学生查询后排序
            if (info.studentId != null) {
                grades = gradeService.getGradesByStudent(info.studentId);
            } else {
                grades = gradeService.getGradesByStudentName(info.studentName);
            }
        } else if (info.subject != null && info.semester != null) {
            grades = gradeService.getGradesBySubjectAndSemester(info.subject, info.semester);
        } else if (info.subject != null) {
            grades = gradeService.getGradesBySubject(info.subject);
        } else if (info.semester != null) {
            grades = gradeService.getGradesBySemester(info.semester);
        } else {
            return "请指定要排序的科目、学期或学生。例如：'按数学成绩排序' 或 '把李杰的成绩排序'";
        }

        if (grades.isEmpty()) {
            return "未找到相关成绩数据。";
        }

        // 按分数排序
        boolean ascending = "asc".equalsIgnoreCase(info.sortOrder) || request.contains("从低到高") || request.contains("升序");
        List<Grade> sortedGrades = gradeService.sortGradesByScore(grades, ascending);

        // 更新上下文
        updateContext(userId, info);

        // 构建响应
        StringBuilder response = new StringBuilder();
        response.append("成绩排序结果（").append(ascending ? "从低到高" : "从高到低").append("）：\n");
        for (int i = 0; i < sortedGrades.size(); i++) {
            Grade grade = sortedGrades.get(i);
            response.append(String.format("%d. %s - %s: %.2f分（%s）\n",
                    i + 1, grade.getStudent().getName(), grade.getSubject(),
                    grade.getScore(), grade.getSemester()));
        }

        return response.toString();
    }

    private String handleAddGradeWithDeepSeek(String request, Context context, String userId) {
        ExtractedInfo info = extractInfoWithDeepSeek(request, "添加成绩", context);

        // 如果没有指定学生，使用上下文中的学生
        if (info.studentId == null && info.studentName == null) {
            info.studentId = context.studentId;
            info.studentName = context.studentName;
        }
        // 如果没有指定科目，使用上下文中的科目
        if (info.subject == null) {
            info.subject = context.subject;
        }
        // 如果没有指定学期，使用上下文中的学期
        if (info.semester == null) {
            info.semester = context.semester;
        }

        // 确定使用studentId还是studentName
        String targetStudentId = info.studentId;
        String targetStudentName = info.studentName;

        if (targetStudentId == null && targetStudentName == null) {
            return "❌ 无法识别学生信息。请提供学生ID或姓名。\n例如：'添加学生1001的数学成绩95分' 或 '添加李杰的数学成绩95分'";
        }

        if (info.subject == null || info.score == null) {
            return "❌ 无法识别科目或分数。请提供科目和分数。\n例如：'添加李杰的数学成绩95分'";
        }

        // 如果学期为空，使用默认学期
        String semester = info.semester != null ? info.semester : "2024年上学期";

        // 如果没有studentId，尝试通过姓名查找或创建
        String studentId = targetStudentId;
        if (studentId == null && targetStudentName != null) {
            // 尝试查找现有学生
            List<Grade> existingGrades = gradeService.getGradesByStudentName(targetStudentName);
            if (!existingGrades.isEmpty()) {
                studentId = existingGrades.get(0).getStudent().getStudentId();
            } else {
                // 创建新学生，使用姓名拼音或随机ID
                studentId = String.valueOf(1000 + (int)(Math.random() * 9000));
            }
        }

        // 添加成绩
        Grade grade = gradeService.addGrade(studentId, info.subject, info.score, semester);

        // 更新上下文
        updateContext(userId, info);

        return String.format("✅ 成功添加成绩：\n学生：%s（%s）\n科目：%s\n分数：%.2f分\n学期：%s",
                grade.getStudent().getName(), grade.getStudent().getStudentId(),
                grade.getSubject(), grade.getScore(), grade.getSemester());
    }

    private String handleUpdateGradeWithDeepSeek(String request, Context context, String userId) {
        ExtractedInfo info = extractInfoWithDeepSeek(request, "修改成绩", context);

        // 如果没有指定学生，使用上下文中的学生
        if (info.studentId == null && info.studentName == null) {
            info.studentId = context.studentId;
            info.studentName = context.studentName;
        }
        // 如果没有指定科目，使用上下文中的科目
        if (info.subject == null) {
            info.subject = context.subject;
        }
        // 如果没有指定学期，使用上下文中的学期
        if (info.semester == null) {
            info.semester = context.semester;
        }

        // 确定使用studentId还是studentName
        String targetStudentId = info.studentId;
        String targetStudentName = info.studentName;

        if (targetStudentId == null && targetStudentName == null) {
            return "❌ 无法识别学生信息。请提供学生ID或姓名。\n例如：'把李杰的物理改成95分'";
        }

        if (info.subject == null || info.newScore == null) {
            return "❌ 无法识别科目或新分数。请提供科目和新分数。\n例如：'把李杰的物理改成95分'";
        }

        // 查找学生成绩
        List<Grade> grades;
        if (targetStudentId != null) {
            grades = gradeService.getGradesByStudent(targetStudentId);
        } else {
            grades = gradeService.getGradesByStudentName(targetStudentName);
        }

        if (grades.isEmpty()) {
            return "❌ 未找到该学生的成绩记录。";
        }

        // 查找指定科目的成绩
        Grade targetGrade = null;
        for (Grade grade : grades) {
            if (grade.getSubject().equals(info.subject)) {
                if (info.semester == null || grade.getSemester().equals(info.semester)) {
                    targetGrade = grade;
                    break;
                }
            }
        }

        if (targetGrade == null) {
            return String.format("❌ 未找到%s的%s成绩记录。",
                    targetStudentName != null ? targetStudentName : "学生" + targetStudentId, info.subject);
        }

        // 保存旧分数用于显示
        double oldScore = targetGrade.getScore();

        // 更新成绩
        targetGrade.setScore(info.newScore);

        // 保存到数据库
        gradeRepository.save(targetGrade);
        logger.info("[EduAgent] 成绩已保存到数据库: {} - {}: {}分 → {}分",
                targetGrade.getStudent().getName(),
                targetGrade.getSubject(),
                oldScore,
                info.newScore);

        // 更新上下文
        updateContext(userId, info);

        return String.format("✅ 成功修改成绩：\n学生：%s\n科目：%s\n原分数：%.2f分 → 新分数：%.2f分\n学期：%s",
                targetGrade.getStudent().getName(),
                targetGrade.getSubject(),
                oldScore,
                info.newScore,
                targetGrade.getSemester());
    }

    private String handleDeleteGradeWithDeepSeek(String request, Context context, String userId) {
        ExtractedInfo info = extractInfoWithDeepSeek(request, "删除成绩", context);

        // 如果没有指定学生，使用上下文中的学生
        if (info.studentId == null && info.studentName == null) {
            info.studentId = context.studentId;
            info.studentName = context.studentName;
        }
        // 如果没有指定科目，使用上下文中的科目
        if (info.subject == null) {
            info.subject = context.subject;
        }
        // 如果没有指定学期，使用上下文中的学期
        if (info.semester == null) {
            info.semester = context.semester;
        }

        // 确定使用studentId还是studentName
        String targetStudentId = info.studentId;
        String targetStudentName = info.studentName;

        if (targetStudentId == null && targetStudentName == null) {
            return "❌ 无法识别学生信息。请提供学生ID或姓名。\n例如：'删除李杰的化学成绩'";
        }

        if (info.subject == null) {
            return "❌ 无法识别科目。请提供科目。\n例如：'删除李杰的化学成绩'";
        }

        // 查找学生成绩
        List<Grade> grades;
        if (targetStudentId != null) {
            grades = gradeService.getGradesByStudent(targetStudentId);
        } else {
            grades = gradeService.getGradesByStudentName(targetStudentName);
        }

        if (grades.isEmpty()) {
            return "❌ 未找到该学生的成绩记录。";
        }

        // 查找指定科目的成绩
        Grade targetGrade = null;
        for (Grade grade : grades) {
            if (grade.getSubject().equals(info.subject)) {
                if (info.semester == null || grade.getSemester().equals(info.semester)) {
                    targetGrade = grade;
                    break;
                }
            }
        }

        if (targetGrade == null) {
            return String.format("❌ 未找到%s的%s成绩记录。",
                    targetStudentName != null ? targetStudentName : "学生" + targetStudentId, info.subject);
        }

        // 保存成绩信息用于显示
        String studentName = targetGrade.getStudent().getName();
        String subject = targetGrade.getSubject();
        double score = targetGrade.getScore();
        String semester = targetGrade.getSemester();

        // 删除成绩
        gradeRepository.delete(targetGrade);
        logger.info("[EduAgent] 成绩已删除: {} - {}: {}分（{}）",
                studentName, subject, score, semester);

        // 更新上下文
        updateContext(userId, info);

        return String.format("✅ 成功删除成绩：\n学生：%s\n科目：%s\n分数：%.2f分\n学期：%s",
                studentName, subject, score, semester);
    }

    private String handleQueryWithDeepSeek(String request, Context context, String userId) {
        ExtractedInfo info = extractInfoWithDeepSeek(request, "查询", context);

        List<Grade> grades;
        String queryDesc;

        // 根据查询类型执行不同的查询
        if ("student".equals(info.queryType) || info.studentId != null || info.studentName != null) {
            // 按学生查询
            if (info.studentId != null) {
                grades = gradeService.getGradesByStudent(info.studentId);
                queryDesc = "学生ID: " + info.studentId;
            } else {
                grades = gradeService.getGradesByStudentName(info.studentName);
                queryDesc = "学生姓名: " + info.studentName;
            }
        } else if ("subject".equals(info.queryType) && info.subject != null) {
            // 按科目查询
            if (info.semester != null) {
                grades = gradeService.getGradesBySubjectAndSemester(info.subject, info.semester);
                queryDesc = "科目: " + info.subject + ", 学期: " + info.semester;
            } else {
                grades = gradeService.getGradesBySubject(info.subject);
                queryDesc = "科目: " + info.subject;
            }
        } else if ("semester".equals(info.queryType) && info.semester != null) {
            // 按学期查询
            grades = gradeService.getGradesBySemester(info.semester);
            queryDesc = "学期: " + info.semester;
        } else if ("all".equals(info.queryType)) {
            // 查询所有
            grades = gradeService.getGradesBySubject("数学");
            grades.addAll(gradeService.getGradesBySubject("语文"));
            grades.addAll(gradeService.getGradesBySubject("英语"));
            queryDesc = "所有科目";
        } else {
            // 默认尝试各种条件
            if (info.studentId != null) {
                grades = gradeService.getGradesByStudent(info.studentId);
                queryDesc = "学生ID: " + info.studentId;
            } else if (info.studentName != null) {
                grades = gradeService.getGradesByStudentName(info.studentName);
                queryDesc = "学生姓名: " + info.studentName;
            } else if (info.subject != null) {
                grades = gradeService.getGradesBySubject(info.subject);
                queryDesc = "科目: " + info.subject;
            } else if (info.semester != null) {
                grades = gradeService.getGradesBySemester(info.semester);
                queryDesc = "学期: " + info.semester;
            } else {
                return "❌ 无法识别查询条件。请提供学生姓名/ID、科目或学期。\n例如：'查询李杰的所有成绩' 或 '查询数学成绩'";
            }
        }

        // 更新上下文
        updateContext(userId, info);

        return formatGradeResults(grades, queryDesc);
    }

    private String handleAnalysisWithDeepSeek(String request, Context context, String userId) {
        ExtractedInfo info = extractInfoWithDeepSeek(request, "分析", context);

        // 如果没有指定学生，使用上下文中的学生
        if (info.studentName == null && info.studentId == null) {
            info.studentName = context.studentName;
            info.studentId = context.studentId;
        }
        // 如果没有指定科目，使用上下文中的科目
        if (info.subject == null) {
            info.subject = context.subject;
        }
        // 如果没有指定学期，使用上下文中的学期
        if (info.semester == null) {
            info.semester = context.semester;
        }

        List<Grade> grades;
        String analysisTarget;

        // 根据分析类型执行不同的分析
        if (info.studentId != null) {
            grades = gradeService.getGradesByStudent(info.studentId);
            analysisTarget = "学生ID: " + info.studentId;
        } else if (info.studentName != null) {
            grades = gradeService.getGradesByStudentName(info.studentName);
            analysisTarget = "学生姓名: " + info.studentName;
        } else if (info.subject != null && info.semester != null) {
            grades = gradeService.getGradesBySubjectAndSemester(info.subject, info.semester);
            analysisTarget = "科目: " + info.subject + ", 学期: " + info.semester;
        } else if (info.subject != null) {
            grades = gradeService.getGradesBySubject(info.subject);
            analysisTarget = "科目: " + info.subject;
        } else if (info.semester != null) {
            grades = gradeService.getGradesBySemester(info.semester);
            analysisTarget = "学期: " + info.semester;
        } else {
            // 获取所有成绩
            grades = gradeService.getGradesBySubject("数学");
            grades.addAll(gradeService.getGradesBySubject("语文"));
            grades.addAll(gradeService.getGradesBySubject("英语"));
            analysisTarget = "所有科目";
        }

        if (grades.isEmpty()) {
            return "📭 没有可分析的成绩数据。请先添加一些成绩。";
        }

        // 更新上下文
        updateContext(userId, info);

        return generateAnalysisReport(grades, analysisTarget);
    }

    private String generateAnalysisReport(List<Grade> grades, String analysisTarget) {
        // 计算统计数据
        double totalScore = 0;
        double maxScore = Double.MIN_VALUE;
        double minScore = Double.MAX_VALUE;
        int excellent = 0; // 90分以上
        int good = 0;      // 80-89分
        int medium = 0;    // 70-79分
        int pass = 0;      // 60-69分
        int fail = 0;      // 60分以下
        Grade maxGrade = null;
        Grade minGrade = null;

        for (Grade grade : grades) {
            double score = grade.getScore();
            totalScore += score;

            if (score > maxScore) {
                maxScore = score;
                maxGrade = grade;
            }
            if (score < minScore) {
                minScore = score;
                minGrade = grade;
            }

            if (score >= 90) excellent++;
            else if (score >= 80) good++;
            else if (score >= 70) medium++;
            else if (score >= 60) pass++;
            else fail++;
        }

        double average = totalScore / grades.size();

        StringBuilder response = new StringBuilder();
        response.append("📊 成绩分析报告\n");
        response.append("==================\n");
        response.append("分析对象：").append(analysisTarget).append("\n\n");

        response.append("📈 基本统计：\n");
        response.append(String.format("• 记录数：%d条\n", grades.size()));
        response.append(String.format("• 平均分：%.2f分\n", average));
        if (maxGrade != null) {
            response.append(String.format("• 最高分：%.2f分（%s - %s）\n",
                    maxScore, maxGrade.getStudent().getName(), maxGrade.getSubject()));
        }
        if (minGrade != null) {
            response.append(String.format("• 最低分：%.2f分（%s - %s）\n",
                    minScore, minGrade.getStudent().getName(), minGrade.getSubject()));
        }

        response.append("\n📊 分数段分布：\n");
        response.append(String.format("• 优秀（90-100分）：%d人 (%.1f%%)\n", excellent, 100.0 * excellent / grades.size()));
        response.append(String.format("• 良好（80-89分）：%d人 (%.1f%%)\n", good, 100.0 * good / grades.size()));
        response.append(String.format("• 中等（70-79分）：%d人 (%.1f%%)\n", medium, 100.0 * medium / grades.size()));
        response.append(String.format("• 及格（60-69分）：%d人 (%.1f%%)\n", pass, 100.0 * pass / grades.size()));
        response.append(String.format("• 不及格（<60分）：%d人 (%.1f%%)\n", fail, 100.0 * fail / grades.size()));

        // 及格率
        double passRate = 100.0 * (excellent + good + medium + pass) / grades.size();
        response.append(String.format("\n✅ 及格率：%.1f%%\n", passRate));

        // 优秀率
        double excellentRate = 100.0 * excellent / grades.size();
        response.append(String.format("🏆 优秀率：%.1f%%\n", excellentRate));

        return response.toString();
    }

    private String formatGradeResults(List<Grade> grades, String queryDesc) {
        if (grades.isEmpty()) {
            return "📭 未找到符合条件的成绩数据。（" + queryDesc + "）";
        }

        // 构建响应
        StringBuilder response = new StringBuilder();
        response.append("📊 查询结果（").append(queryDesc).append("，共").append(grades.size()).append("条记录）：\n\n");

        double totalScore = 0;
        double maxScore = Double.MIN_VALUE;
        double minScore = Double.MAX_VALUE;
        Grade maxGrade = null;
        Grade minGrade = null;

        for (Grade grade : grades) {
            response.append(String.format("• %s - %s: %.2f分（%s）\n",
                    grade.getStudent().getName(), grade.getSubject(),
                    grade.getScore(), grade.getSemester()));

            totalScore += grade.getScore();
            if (grade.getScore() > maxScore) {
                maxScore = grade.getScore();
                maxGrade = grade;
            }
            if (grade.getScore() < minScore) {
                minScore = grade.getScore();
                minGrade = grade;
            }
        }

        // 添加统计信息
        response.append(String.format("\n📈 统计信息：\n"));
        response.append(String.format("平均分：%.2f分\n", totalScore / grades.size()));
        if (maxGrade != null) {
            response.append(String.format("最高分：%.2f分（%s - %s）\n",
                    maxScore, maxGrade.getSubject(), maxGrade.getSemester()));
        }
        if (minGrade != null) {
            response.append(String.format("最低分：%.2f分（%s - %s）\n",
                    minScore, minGrade.getSubject(), minGrade.getSemester()));
        }

        return response.toString();
    }

    private String handleAIGenericRequest(String request, String userId) {
        // 获取用户画像和上下文
        String userPreferences = userProfileService.getUserPreferences(userId).toString();
        Context context = getContext(userId);

        String contextInfo = "";
        if (context.studentName != null) {
            contextInfo += "用户当前关注的学生: " + context.studentName + "\n";
        }
        if (context.subject != null) {
            contextInfo += "用户当前关注的科目: " + context.subject + "\n";
        }

        String systemPrompt = "你是一个专业的教务智能助手，可以帮助教师管理学生成绩、查询信息和进行数据分析。\n" +
                contextInfo +
                "\n你可以执行以下操作：\n" +
                "1. 添加学生成绩\n" +
                "2. 修改学生成绩\n" +
                "3. 查询成绩（按学生、科目、学期）\n" +
                "4. 成绩排序和排名\n" +
                "5. 成绩统计和分析\n\n" +
                "当前用户的偏好设置：" + userPreferences + "\n\n" +
                "请友好、专业地回答用户的问题。如果用户需要执行具体操作，请引导他们使用标准格式。";

        return deepSeekService.chatWithSystemPrompt(systemPrompt, request);
    }

    // 上下文管理方法
    private Context getContext(String userId) {
        Context context = new Context();
        context.studentId = userProfileService.getUserPreference(userId, "context_studentId");
        context.studentName = userProfileService.getUserPreference(userId, "context_studentName");
        context.subject = userProfileService.getUserPreference(userId, "context_subject");
        context.semester = userProfileService.getUserPreference(userId, "context_semester");
        return context;
    }

    private void updateContext(String userId, ExtractedInfo info) {
        if (info.studentId != null) {
            userProfileService.updateUserPreference(userId, "context_studentId", info.studentId);
        }
        if (info.studentName != null) {
            userProfileService.updateUserPreference(userId, "context_studentName", info.studentName);
        }
        if (info.subject != null) {
            userProfileService.updateUserPreference(userId, "context_subject", info.subject);
        }
        if (info.semester != null) {
            userProfileService.updateUserPreference(userId, "context_semester", info.semester);
        }
        logger.info("[EduAgent] 上下文已更新: studentName={}, subject={}, semester={}",
                info.studentName, info.subject, info.semester);
    }

    private void updateUserPreference(String userId, String request) {
        // 分析用户请求类型并更新用户偏好
        String lowerRequest = request.toLowerCase();
        if (lowerRequest.contains("排序") || lowerRequest.contains("排名")) {
            userProfileService.updateUserPreference(userId, "last_action", "sort");
            userProfileService.updateUserPreference(userId, "preferred_feature", "ranking");
        } else if (lowerRequest.contains("插入") || lowerRequest.contains("添加") || lowerRequest.contains("录入")) {
            userProfileService.updateUserPreference(userId, "last_action", "add");
            userProfileService.updateUserPreference(userId, "preferred_feature", "data_entry");
        } else if (lowerRequest.contains("修改") || lowerRequest.contains("更新") || lowerRequest.contains("改成")) {
            userProfileService.updateUserPreference(userId, "last_action", "update");
            userProfileService.updateUserPreference(userId, "preferred_feature", "update");
        } else if (lowerRequest.contains("查询") || lowerRequest.contains("查看")) {
            userProfileService.updateUserPreference(userId, "last_action", "query");
            userProfileService.updateUserPreference(userId, "preferred_feature", "query");
        } else if (lowerRequest.contains("分析") || lowerRequest.contains("统计")) {
            userProfileService.updateUserPreference(userId, "last_action", "analysis");
            userProfileService.updateUserPreference(userId, "preferred_feature", "analysis");
        }

        // 记录用户经常查询的科目
        String subject = extractSubject(request);
        if (subject != null) {
            userProfileService.updateUserPreference(userId, "preferred_subject", subject);
        }

        // 记录用户经常查询的学期
        String semester = extractSemester(request);
        if (semester != null) {
            userProfileService.updateUserPreference(userId, "preferred_semester", semester);
        }
    }

    // 辅助方法：从JSON中提取值
    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^\"\\},]+)\"?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            if (value.equals("null") || value.isEmpty()) {
                return null;
            }
            return value;
        }
        return null;
    }

    // 辅助方法：从请求中提取科目（用于用户画像）
    private String extractSubject(String request) {
        String[] subjects = {"数学", "语文", "英语", "物理", "化学", "生物", "历史", "地理", "政治"};
        for (String subject : subjects) {
            if (request.contains(subject)) {
                return subject;
            }
        }
        return null;
    }

    // 辅助方法：从请求中提取学期（用于用户画像）
    private String extractSemester(String request) {
        Pattern pattern = Pattern.compile("(20\\d{2})[年\\-\\s]*(上|下)学期");
        Matcher matcher = pattern.matcher(request);
        if (matcher.find()) {
            return matcher.group(1) + "年" + matcher.group(2) + "学期";
        }
        return null;
    }

    // 内部类：存储提取的信息
    private static class ExtractedInfo {
        String studentId;
        String studentName;
        String subject;
        Double score;
        Double newScore;
        String semester;
        String sortOrder;
        String queryType;
    }

    // 内部类：上下文信息
    private static class Context {
        String studentId;
        String studentName;
        String subject;
        String semester;

        @Override
        public String toString() {
            return String.format("Context{studentName='%s', subject='%s', semester='%s'}",
                    studentName, subject, semester);
        }
    }
}
