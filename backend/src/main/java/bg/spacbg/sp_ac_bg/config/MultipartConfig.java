package bg.spacbg.sp_ac_bg.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.MultipartFilter;

@Configuration
public class MultipartConfig {

    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilterRegistrationBean() {
        FilterRegistrationBean<MultipartFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new MultipartFilter());
        registrationBean.addUrlPatterns("/graphql");
        registrationBean.setOrder(-100); // Before Spring Security
        return registrationBean;
    }
}
