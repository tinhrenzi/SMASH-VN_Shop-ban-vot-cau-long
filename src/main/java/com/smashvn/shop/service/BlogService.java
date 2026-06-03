package com.smashvn.shop.service;

import com.smashvn.shop.entity.Blog;
import com.smashvn.shop.repository.BlogRepository;
import com.smashvn.shop.dto.BlogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;

    @PostConstruct
    public void initData() {
        if (blogRepository.count() == 0) {
            // Khởi tạo 5 bài viết chuyên sâu về cầu lông
            
            Blog post1 = Blog.builder()
                    .title("Hướng dẫn chọn vợt cầu lông phù hợp cho người mới bắt đầu")
                    .slug("huong-dan-chon-vot-cau-long-cho-nguoi-moi-bat-dau")
                    .summary("Chọn vợt cầu lông phù hợp là bước quan trọng giúp người mới tập chơi tránh chấn thương và nhanh chóng tiến bộ. Bài viết phân tích các yếu tố trọng lượng, độ cứng đũa, và điểm cân bằng của vợt.")
                    .content("<p>Chọn vợt cầu lông phù hợp là bước đi đầu tiên và cực kỳ quan trọng đối với bất kỳ ai khi mới bước chân vào bộ môn này. Một cây vợt không thích hợp không chỉ hạn chế kỹ thuật của bạn mà còn có thể dẫn đến những chấn thương không đáng có như đau cổ tay, viêm khớp vai hoặc khuỷu tay.</p>" +
                             "<h3>1. Trọng lượng vợt (Thông số chữ U)</h3>" +
                             "<p>Thông số chữ U trên thân vợt quy định trọng lượng của cây vợt đó. Số U càng lớn thì vợt càng nhẹ và ngược lại:</p>" +
                             "<ul>" +
                             "<li><strong>3U (85g - 89g):</strong> Phù hợp cho người chơi có cổ tay khỏe, thường là nam giới chơi lâu năm.</li>" +
                             "<li><strong>4U (80g - 84g):</strong> Mức trọng lượng lý tưởng nhất cho đa số người chơi phong trào, bao gồm cả nam giới mới chơi.</li>" +
                             "<li><strong>5U (75g - 79g) hoặc nhẹ hơn:</strong> Dành cho người có lực cổ tay yếu, nữ giới hoặc người thiên về thủ cầu, phản tạt nhanh.</li>" +
                             "</ul>" +
                             "<h3>2. Điểm cân bằng của vợt (Balance Point)</h3>" +
                             "<p>Điểm cân bằng quyết định lối chơi của cây vợt:</p>" +
                             "<ul>" +
                             "<li><strong>Vợt nhẹ đầu (Defensive / Light Head):</strong> Phù hợp cho lối chơi phòng thủ, điều cầu linh hoạt.</li>" +
                             "<li><strong>Vợt cân bằng (All-around / Even Balance):</strong> Lối đánh công thủ toàn diện, rất thích hợp cho người mới bắt đầu để làm quen với mọi kỹ thuật.</li>" +
                             "<li><strong>Vợt nặng đầu (Offensive / Heavy Head):</strong> Hỗ trợ đập cầu mạnh mẽ, dành cho người có lối đánh tấn công uy lực.</li>" +
                             "</ul>" +
                             "<h3>3. Độ cứng của thân vợt (Stiffness)</h3>" +
                             "<p>Đối với người mới chơi, thân vợt dẻo (Flexible) hoặc trung bình (Medium) là lựa chọn tốt nhất nhờ khả năng trợ lực tốt trong các cú phông cầu cuối sân. Tránh chọn các cây vợt thân siêu cứng (Stiff) vì đòi hỏi lực tay cực lớn để phát huy tối đa lực và dễ gây chấn thương.</p>")
                    .image("post-1.jpg")
                    .publishDate(LocalDate.of(2026, 5, 20))
                    .author("Nguyễn Thế Vinh")
                    .category("Hướng Dẫn")
                    .tags("Chọn Vợt,Người Mới,Kỹ Thuật Cầu Lông")
                    .commentsCount(12)
                    .build();

            Blog post2 = Blog.builder()
                    .title("Top 5 cây vợt cầu lông Yonex đáng mua nhất năm 2026")
                    .slug("top-5-cay-vot-cau-long-yonex-dang-mua-nhat-nam-2026")
                    .summary("Yonex luôn là ông vua trong làng cầu lông thế giới. Khám phá ngay danh sách 5 cây vợt Yonex được săn đón nhất năm 2026 với các công nghệ đột phá cải thiện tối đa hiệu suất thi đấu.")
                    .content("<p>Yonex tiếp tục khẳng định vị thế dẫn đầu trong năm 2026 bằng việc ra mắt và cải tiến các dòng vợt huyền thoại. Nếu bạn đang tìm kiếm một cây vợt Yonex đỉnh cao để nâng tầm kỹ năng của mình, dưới đây là top 5 sự lựa chọn xuất sắc nhất:</p>" +
                             "<h3>1. Yonex Astrox 88D Pro (Thế hệ mới)</h3>" +
                             "<p>Được thiết kế riêng cho người chơi đứng sau trong đánh đôi, Astrox 88D Pro mang lại những cú đập cầu cắm sân với lực công phá vô song. Trục vợt được cải tiến giúp tăng thời gian giữ cầu trên mặt vợt, tạo độ kiểm soát hoàn hảo.</p>" +
                             "<h3>2. Yonex Astrox 77 Pro</h3>" +
                             "<p>Cực kỳ dễ thuần và linh hoạt. Đây là cây vợt tấn công nhẹ nhàng, trợ lực tốt và thân thiện với cổ tay người chơi phong trào. Lựa chọn tuyệt vời cho cả đánh đơn và đánh đôi.</p>" +
                             "<h3>3. Yonex Nanoflare 1000Z</h3>" +
                             "<p>Được mệnh danh là \"tia chớp\" của Yonex với khả năng vung vợt siêu nhanh. Vợt nặng đầu nhẹ nhưng khung khí động học cao, đem lại khả năng phản tạt và đè cầu chớp nhoáng trên lưới.</p>" +
                             "<h3>4. Yonex Arcsaber 11 Pro</h3>" +
                             "<p>Dòng vợt kiểm soát cầu huyền thoại. Thân vợt có độ cứng trung bình cùng điểm cân bằng lý tưởng giúp người chơi thực hiện các cú điều cầu chuẩn xác đến từng centimet.</p>" +
                             "<h3>5. Yonex Astrox 99 Pro</h3>" +
                             "<p>Vũ khí tối thượng của các tay đập đơn. Với thiết kế nặng đầu vượt trội, Astrox 99 Pro mang lại sức mạnh đập cầu hủy diệt cho lối đánh tấn công uy lực.</p>")
                    .image("post-2.jpg")
                    .publishDate(LocalDate.of(2026, 5, 25))
                    .author("Trần Minh Quân")
                    .category("Review")
                    .tags("Yonex,Astrox,Nanoflare,Arcsaber")
                    .commentsCount(8)
                    .build();

            Blog post3 = Blog.builder()
                    .title("Kỹ thuật giao cầu lông chuẩn xác và cách khắc phục lỗi thường gặp")
                    .slug("ky-thuat-giao-cau-long-chuan-xac-va-cach-khac-phuc-loi-thuong-gap")
                    .summary("Giao cầu là khởi đầu của mọi đường bóng. Nắm vững kỹ thuật giao cầu ngắn và giao cầu cao sâu giúp bạn làm chủ thế trận ngay từ cú đánh đầu tiên và tránh bị đối thủ bắt bài.")
                    .content("<p>Trong cầu lông, giao cầu không đơn thuần là đưa quả cầu sang sân đối phương mà là một vũ khí chiến thuật quan trọng. Một cú giao cầu tốt sẽ đặt đối thủ vào thế bị động, giúp bạn giành quyền chủ động tấn công ngay lập tức.</p>" +
                             "<h3>1. Kỹ thuật giao cầu ngắn (Giao cầu dưới tay)</h3>" +
                             "<p>Thường dùng phổ biến trong đánh đôi để hạn chế đối phương tấn công trực diện. Kỹ thuật yêu cầu:</p>" +
                             "<ul>" +
                             "<li>Đứng sát vạch giao cầu phát bóng ngắn khoảng 10-20 cm.</li>" +
                             "<li>Cầm vợt nhẹ nhàng bằng ngón cái ôm sát mặt cán lớn (cách cầm vợt trái tay).</li>" +
                             "<li>Đẩy nhẹ cầu bằng lực cổ tay, sao cho quả cầu đi sát mép trên của lưới và rơi ngay vạch giao cầu ngắn của đối thủ.</li>" +
                             "</ul>" +
                             "<h3>2. Kỹ thuật giao cầu cao sâu</h3>" +
                             "<p>Thường áp dụng trong đánh đơn nhằm đẩy đối thủ lùi sâu về cuối sân. Cách thực hiện:</p>" +
                             "<ul>" +
                             "<li>Đứng cách vạch giao cầu khoảng 1 mét.</li>" +
                             "<li>Sử dụng động tác giao cầu thuận tay, mở rộng cánh tay và sử dụng lực xoay hông cùng cổ tay phát lực từ dưới lên trên, ra trước.</li>" +
                             "<li>Điểm rơi của cầu phải sát vạch giới hạn cuối sân của đối phương.</li>" +
                             "</ul>" +
                             "<h3>3. Các lỗi thường gặp và cách khắc phục</h3>" +
                             "<p><strong>Lỗi giao cầu quá cao trên lưới:</strong> Thường do lực đẩy quá mạnh hoặc góc tiếp xúc của mặt vợt ngửa quá nhiều. Khắc phục bằng cách tập trung điều khiển ngón tay cái và giữ mặt vợt hơi nghiêng khi tiếp xúc cầu.</p>" +
                             "<p><strong>Lỗi phạm quy (giao cầu quá thắt lưng):</strong> Theo luật mới, toàn bộ quả cầu phải dưới 1.15m tại thời điểm tiếp xúc vợt. Hãy điều chỉnh vị trí thả cầu thấp hơn sườn của bạn.</p>")
                    .image("post-3.jpg")
                    .publishDate(LocalDate.of(2026, 5, 28))
                    .author("Phan Hoàng Nam")
                    .category("Kỹ Thuật")
                    .tags("Giao Cầu,Kỹ Thuật Đơn,Kỹ Thuật Đôi")
                    .commentsCount(15)
                    .build();

            Blog post4 = Blog.builder()
                    .title("Cách căng dây vợt cầu lông và lựa chọn mức căng (lbs) phù hợp")
                    .slug("cach-cang-day-vot-cau-long-va-lua-chon-muc-cang-lbs-phu-hop")
                    .summary("Mức căng dây quyết định đến lực đẩy và cảm giác cầu của bạn. Đọc bài viết để hiểu rõ nên chọn mức căng bao nhiêu lbs là tối ưu cho trình độ và lực tay hiện tại của bạn.")
                    .content("<p>Mức căng vợt cầu lông (tính bằng lbs hoặc kg) ảnh hưởng trực tiếp tới 80% cảm giác cầu và khả năng kiểm soát đường bay của quả cầu. Nhiều người chơi thường có xu hướng căng mức cân quá cao theo thần tượng mà không biết rằng việc đó gây hại rất lớn cho cổ tay.</p>" +
                             "<h3>1. Hiểu về thông số căng dây (Lbs/Kg)</h3>" +
                             "<p>Mức cân thông thường dao động từ 18 lbs đến 30 lbs (khoảng 8kg - 13.5kg):</p>" +
                             "<ul>" +
                             "<li><strong>Căng thấp (18 - 21 lbs / 8kg - 9.5kg):</strong> Dây chùng, độ đàn hồi cao giúp trợ lực cực tốt, quả cầu đi xa mà không cần tốn nhiều sức. Tuy nhiên, khả năng kiểm soát hướng đi của cầu kém.</li>" +
                             "<li><strong>Căng trung bình (22 - 24 lbs / 10kg - 11kg):</strong> Mức cân hoàn hảo nhất cho người chơi phong trào có trình độ trung bình. Cân bằng tốt giữa trợ lực và kiểm soát.</li>" +
                             "<li><strong>Căng cao (25 - 28+ lbs / 11.5kg - 13kg):</strong> Dây rất căng, mặt vợt cứng. Hỗ trợ cảm giác cầu chân thật, kiểm soát đường bóng cực tốt và những cú đập cầu đi nhanh, cắm hơn. Nhưng hầu như không trợ lực, đòi hỏi người chơi có lực cổ tay cực khỏe.</li>" +
                             "</ul>" +
                             "<h3>2. Lựa chọn mức căng phù hợp theo trình độ</h3>" +
                             "<ul>" +
                             "<li><strong>Người mới chơi, học sinh, nữ giới:</strong> Nên bắt đầu từ 20 - 21 lbs để làm quen và bảo vệ khớp cổ tay.</li>" +
                             "<li><strong>Người chơi phong trào từ 1 - 2 năm:</strong> Mức căng từ 22 - 23 lbs là tối ưu nhất.</li>" +
                             "<li><strong>Người chơi bán chuyên, phủi cứng:</strong> Thường lựa chọn mức căng từ 24 - 26 lbs để thực hiện các cú đánh kỹ thuật cao.</li>" +
                             "</ul>" +
                             "<h3>3. Lưu ý quan trọng</h3>" +
                             "<p>Mỗi khung vợt đều ghi rõ giới hạn chịu lực căng tối đa (tension limit). Không bao giờ căng vượt quá thông số này của nhà sản xuất vì có thể làm móp méo hoặc sập khung vợt.</p>")
                    .image("post-4.jpg")
                    .publishDate(LocalDate.of(2026, 6, 1))
                    .author("Nguyễn Thế Vinh")
                    .category("Kỹ Thuật")
                    .tags("Căng Vợt,Lbs,Dây Cầu Lông")
                    .commentsCount(19)
                    .build();

            Blog post5 = Blog.builder()
                    .title("Review chi tiết dòng vợt Lining Tectonic 7 - Sức mạnh tấn công vượt trội")
                    .slug("review-chi-tiet-dong-vot-lining-tectonic-7-suc-manh-tan-cong-vuot-troi")
                    .summary("Lining Tectonic 7 nổi bật với công nghệ khung hộp đàn hồi độc đáo giúp tăng tốc độ phản hồi lực đập. Bài đánh giá chân thực về ưu và nhược điểm của siêu phẩm tấn công này.")
                    .content("<p>Dòng vợt Lining Tectonic 7 từ lâu đã nổi danh là một trong những vũ khí tấn công mạnh mẽ nhất được nhiều tay vợt chuyên nghiệp thế giới tin dùng. Với thiết kế khung dạng hộp đàn hồi độc đáo ở góc 5 giờ và 7 giờ, cây vợt này mang lại tốc độ phục hồi đáng kinh ngạc sau mỗi cú vung vợt.</p>" +
                             "<h3>1. Thông số kỹ thuật ấn tượng</h3>" +
                             "<ul>" +
                             "<li><strong>Trọng lượng:</strong> 3U (khoảng 88g) hoặc 4U (khoảng 83g).</li>" +
                             "<li><strong>Điểm cân bằng:</strong> 295mm (hơi nặng đầu), hỗ trợ đập cầu tấn công tốt.</li>" +
                             "<li><strong>Độ cứng thân vợt:</strong> Trung bình, dẻo hơn so với dòng Yonex Astrox 99, giúp dễ tiếp cận hơn.</li>" +
                             "</ul>" +
                             "<h3>2. Trải nghiệm thực tế khi thi đấu</h3>" +
                             "<p><strong>Khả năng tấn công:</strong> Tectonic 7 mang đến những cú smash vô cùng uy lực. Đầu vợt hơi đầm giúp giữ nhịp đập liên tục mà không gây mỏi tay nhờ khung phục hồi trạng thái cực nhanh.</p>" +
                             "<p><strong>Khả năng phòng thủ phản tạt:</strong> Nhờ thiết kế khí động học cải tiến, mặc dù là vợt tấn công nhưng Tectonic 7 vẫn mang lại tốc độ xoay trở trên lưới tương đối linh hoạt, không bị chậm nhịp trong các pha thủ cầu sâu.</p>" +
                             "<h3>3. Đối tượng phù hợp</h3>" +
                             "<p>Tectonic 7 bản 4U phù hợp cho người chơi phong trào trình độ trung bình - khá trở lên, ưa thích lối đánh tấn công uy lực từ cuối sân. Phiên bản 3U sẽ đòi hỏi người chơi có thể lực dồi dào và cổ tay tốt để làm chủ hoàn toàn.</p>")
                    .image("post-5.jpg")
                    .publishDate(LocalDate.of(2026, 6, 2))
                    .author("Lê Huy Hoàng")
                    .category("Review")
                    .tags("Lining,Tectonic 7,Review Vợt")
                    .commentsCount(6)
                    .build();

            blogRepository.saveAll(Arrays.asList(post1, post2, post3, post4, post5));
        }
    }

    public Page<BlogDTO> getBlogs(String query, Pageable pageable) {
        Page<Blog> blogs;
        if (query != null && !query.trim().isEmpty()) {
            blogs = blogRepository.findByTitleContainingIgnoreCaseOrSummaryContainingIgnoreCaseOrContentContainingIgnoreCase(
                    query, query, query, pageable);
        } else {
            blogs = blogRepository.findAll(pageable);
        }
        return blogs.map(this::convertToDTO);
    }

    public BlogDTO getBlogBySlug(String slug) {
        Blog blog = blogRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết nào với đường dẫn: " + slug));
        return convertToDTO(blog);
    }

    public java.util.List<BlogDTO> getRecentBlogs(int limit) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "publishDate"));
        return blogRepository.findAll(pageable).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private BlogDTO convertToDTO(Blog blog) {
        List<String> tagList = Collections.emptyList();
        if (blog.getTags() != null && !blog.getTags().trim().isEmpty()) {
            tagList = Arrays.stream(blog.getTags().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
        
        return BlogDTO.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .summary(blog.getSummary())
                .content(blog.getContent())
                .image(blog.getImage())
                .publishDate(blog.getPublishDate() != null ? blog.getPublishDate().toString() : "")
                .author(blog.getAuthor())
                .category(blog.getCategory())
                .tags(tagList)
                .commentsCount(blog.getCommentsCount())
                .build();
    }
}
