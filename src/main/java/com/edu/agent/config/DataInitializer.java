package com.edu.agent.config;

import com.edu.agent.model.Grade;
import com.edu.agent.model.Student;
import com.edu.agent.repository.GradeRepository;
import com.edu.agent.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private GradeRepository gradeRepository;
    
    private final Random random = new Random();
    
    private final List<String> subjects = Arrays.asList("数学", "语文", "英语", "物理", "化学");
    private final List<String> semesters = Arrays.asList("2023年上学期", "2023年下学期", "2024年上学期");
    
    @Override
    public void run(String... args) {
        logger.info("========== 开始初始化模拟数据 ==========");
        
        // 检查是否已有数据
        long studentCount = studentRepository.count();
        if (studentCount > 0) {
            logger.info("数据库中已有 {} 名学生，跳过数据初始化", studentCount);
            return;
        }
        
        // 创建学生
        logger.info("正在创建学生数据...");
        createStudents();
        
        // 创建成绩
        logger.info("正在创建成绩数据...");
        createGrades();
        
        logger.info("========== 模拟数据初始化完成 ==========");
        logger.info("共创建 {} 名学生", studentRepository.count());
        logger.info("共创建 {} 条成绩记录", gradeRepository.count());
    }
    
    private void createStudents() {
        String[] lastNames = {"张", "李", "王", "刘", "陈", "杨", "赵", "黄", "周", "吴"};
        String[] firstNames = {"伟", "芳", "娜", "敏", "静", "强", "磊", "洋", "艳", "杰", "勇", "军", "平", "刚", "桂英"};
        String[] classNames = {"高一(1)班", "高一(2)班", "高一(3)班", "高二(1)班", "高二(2)班"};
        
        for (int i = 1; i <= 20; i++) {
            Student student = new Student();
            student.setStudentId(String.valueOf(1000 + i));
            String name = lastNames[random.nextInt(lastNames.length)] + firstNames[random.nextInt(firstNames.length)];
            student.setName(name);
            student.setClassName(classNames[random.nextInt(classNames.length)]);
            
            studentRepository.save(student);
            logger.info("创建学生: {} (ID: {}, 班级: {})", name, student.getStudentId(), student.getClassName());
        }
    }
    
    private void createGrades() {
        List<Student> students = studentRepository.findAll();
        
        for (Student student : students) {
            // 为每个学生随机生成2-5门课程的成绩
            int numGrades = random.nextInt(4) + 2;
            
            for (int i = 0; i < numGrades; i++) {
                Grade grade = new Grade();
                grade.setStudent(student);
                grade.setSubject(subjects.get(random.nextInt(subjects.size())));
                
                // 生成60-100分的成绩，正态分布
                double score = generateNormalScore();
                grade.setScore(score);
                
                grade.setSemester(semesters.get(random.nextInt(semesters.size())));
                
                gradeRepository.save(grade);
                logger.info("创建成绩: {} - {}: {}分 ({})", 
                    student.getName(), grade.getSubject(), score, grade.getSemester());
            }
        }
    }
    
    private double generateNormalScore() {
        // 使用正态分布生成成绩，平均分75，标准差10
        double score = 75 + random.nextGaussian() * 10;
        // 限制在60-100之间
        score = Math.max(60, Math.min(100, score));
        // 保留一位小数
        return Math.round(score * 10) / 10.0;
    }
}