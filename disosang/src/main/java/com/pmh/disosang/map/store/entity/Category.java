package com.pmh.disosang.map.store.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    // 부모 카테고리 (Self Join)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false)
    private int depth;

    @Column(name = "group_code", nullable = false, length = 30)
    private String groupCode;


    public void changeParent(Category parent) {
        this.parent = parent;
    }


}
