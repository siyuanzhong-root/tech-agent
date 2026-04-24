package com.edu.agent.repository;

import com.edu.agent.model.Grade;
import com.edu.agent.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudent(Student student);
    List<Grade> findBySubject(String subject);
    List<Grade> findBySemester(String semester);
}