package com.org.llm.tool;

import com.org.llm.model.Contact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactsTool {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    @Tool(description = "Find contacts in a given city")
    public List<Contact> findContactsByCity(String city) {
        log.info("Finding contacts in city: " + city);
        String sql = "SELECT name, email, city FROM contacts WHERE city = ?";

        RowMapper<Contact> rowMapper = (rs, rowNum) ->
                new Contact(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("city")
                );

        return jdbcTemplate.query(sql, rowMapper, city);
    }

    @Tool(description = "Formats a list of contacts into CSV with headers: Name, Email, City")
    public String formatAsCsv(List<Contact> contacts) {
        StringBuilder sb = new StringBuilder("Name,Email,City\n");
        for (Contact c : contacts) {
            sb.append(c.name())
                    .append(",")
                    .append(c.email())
                    .append(",")
                    .append(c.city())
                    .append("\n");
        }
        return sb.toString();
    }
}
