package com.edu.agent.service;

import com.edu.agent.model.Grade;
import com.edu.agent.model.Student;
import com.edu.agent.repository.GradeRepository;
import com.edu.agent.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GradeService {
    @Autowired
    private GradeRepository gradeRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    public Grade addGrade(String studentId, String subject, Double score, String semester) {
        Student student = studentRepository.findByStudentId(studentId);
        if (student == null) {
            student = new Student();
            student.setStudentId(studentId);
            student.setName("Student " + studentId); // 默认名称
            student.setClassName("Default Class"); // 默认班级
            student = studentRepository.save(student);
        }
        
        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setSubject(subject);
        grade.setScore(score);
        grade.setSemester(semester);
        
        return gradeRepository.save(grade);
    }
    
    public List<Grade> getGradesByStudent(String studentId) {
        Student student = studentRepository.findByStudentId(studentId);
        if (student == null) {
            return List.of();
        }
        return gradeRepository.findByStudent(student);
    }
    
    public List<Grade> getGradesByStudentName(String studentName) {
        List<Student> students = studentRepository.findByNameContaining(studentName);
        List<Grade> allGrades = new ArrayList<>();
        for (Student student : students) {
            allGrades.addAll(gradeRepository.findByStudent(student));
        }
        return allGrades;
    }
    
    public List<Grade> getGradesBySubject(String subject) {
        return gradeRepository.findBySubject(subject);
    }
    
    public List<Grade> getGradesBySemester(String semester) {
        return gradeRepository.findBySemester(semester);
    }
    
    public List<Grade> getGradesBySubjectAndSemester(String subject, String semester) {
        return gradeRepository.findBySubject(subject)
                .stream()
                .filter(grade -> grade.getSemester().equals(semester))
                .collect(Collectors.toList());
    }
    
    public List<Grade> sortGradesByScore(List<Grade> grades, boolean ascending) {
        return grades.stream()
                .sorted(ascending ? Comparator.comparing(Grade::getScore) : Comparator.comparing(Grade::getScore).reversed())
                .collect(Collectors.toList());
    }
}