package com.yeshwanth.respository;

import com.yeshwanth.entites.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRespository  extends JpaRepository<Product,Long> {
}
